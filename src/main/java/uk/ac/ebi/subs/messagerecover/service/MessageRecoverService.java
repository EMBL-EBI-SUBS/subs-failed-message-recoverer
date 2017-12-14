package uk.ac.ebi.subs.messagerecover.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.messagerecover.config.RecoverProperties;
import uk.ac.ebi.subs.messagerecover.queuemanager.MessageFilter;
import uk.ac.ebi.subs.messagerecover.queuemanager.MessageProperties;
import uk.ac.ebi.subs.messagerecover.queuemanager.MessageToReplay;
import uk.ac.ebi.subs.messagerecover.queuemanager.QDBManager;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This service is responsible for communicating with the QDBManager to create and read queues,
 * add and remove bindings between RabbitMQ and QDB queues and
 * fix and replay the failed messages.
 */
@Component
public class MessageRecoverService {

    private static final Logger logger = LoggerFactory.getLogger(MessageRecoverService.class);

    private QDBManager qdbManager;
    private RecoverProperties recoverProperties;
    private RabbitMessagingTemplate rabbitMessagingTemplate;
    private String qdbQueueName;

    public MessageRecoverService(RecoverProperties recoverProperties, QDBManager qdbManager,
                                 RabbitMessagingTemplate rabbitMessagingTemplate) {
        this.recoverProperties = recoverProperties;
        this.qdbManager = qdbManager;
        this.rabbitMessagingTemplate = rabbitMessagingTemplate;
        this.qdbQueueName = recoverProperties.getQdbProp().getQueue().getDeadLetterQueueName()
                            + "__" + UUID.randomUUID().toString();
    }

    /**
     * Transfer messages from a specified RabbitMQ queue to a QDB queue.
     * First it creates the QDB queue, then it creates an input binding between a RabbitMQ queue and a QDB queue.
     * After a specified amount of time it will remove the above specified input binding
     * to not have the possibility to create a cyclical link (message routing) between RabbitMQ and QDB.
     */
    public void transferMessagesToQDBDeadLetterQueue() {
        createQDBDeadLetterQueue();
        addInputBindingToQDBDeadLetterQueue();
        removeInputBindingFromQDBDeadLetterQueue();
    }

    /**
     * Creates a queue in QDB server for storing the failed messages.
     *
     * @return the name of the created QDB queue
     */
    private void createQDBDeadLetterQueue() {
        qdbManager.createQDBDeadLetterQueue(qdbQueueName);
    }

    /**
     * Add a binding between a RabbitMQ and a QDB queue.
     */
    private void addInputBindingToQDBDeadLetterQueue() {
        String rabbitQueueName = recoverProperties.getRabbitMQProp().getDeadLetterQueueName();
        qdbManager.addInputBindingToQDBDeadLetterQueue(rabbitQueueName, qdbQueueName);
    }

    /**
     * Remove a binding between a RabbitMQ and a QDB queue.
     * Before removing the binding it waits a defined amount of time (configurable in the application.yml file).
     */
    private void removeInputBindingFromQDBDeadLetterQueue() {
        try {
            Thread.sleep(1000 * Integer.valueOf(recoverProperties.getInputBindingRemovalDelayInSec()));
        } catch (InterruptedException e) {
            logger.info("Waiting for removing the input binding is interrupted.\n Error message: {}", e.getMessage());
            throw new RuntimeException(e.getCause());
        }

        String rabbitQueueName = recoverProperties.getRabbitMQProp().getDeadLetterQueueName();
        qdbManager.removeInputBindingFromQDBDeadLetterQueue(rabbitQueueName, qdbQueueName);
    }

    /**
     * Retrieves messages with applied filter criteria defined in the application.yml file.
     *
     * @return a filtered {@link List} of {@link MessageToReplay}
     */
    public List<MessageToReplay> readFilterMessagesFromQDBDaedLetterQueue() {
        ResponseEntity<String> response = qdbManager.filterMessagesFromQDBDeadLetterQueue(qdbQueueName);
        return parseFailedMessages(response.getBody());
    }

    /**
     * This is the method that you probably would have to replace to fix a {@link List} of failed messages.
     *
     * ***** IMPORTANT!!! *****
     * THE CURRENT OR DEFAULT METHOD IS JUST A NAIVE IMPLEMENTATION FOR REPLAY A MESSAGE WHEN A SERVICE WAS DOWN
     * IF THE CASE IS SOMETHING ELSE, THEN THIS METHOD BODY SHOULD BE CHANGED
     * AND IT SHOULD CONTAIN THE FIX OF THE MESSAGES.
     *
     * @param messageToReplay a {@link List} of {@link MessageToReplay} that holds the original failed messages
     *                         and their routing keys
     */
    public void fixFailedMessages(List<MessageToReplay> messageToReplay) {
        logger.info("[MessageRecoverService] fixing messages");

        messageToReplay.forEach(message -> message.setBodyToReplay(message.getBody()));
    }

    /**
     * Replay the fixed/corrected messages with their original routing key to the given RabbitMQ exchange.
     * The exchange is defined in the application.yml configuration file.
     *
     * @param messageToReplay a {@link List} of {@link MessageToReplay}
     */
    public void replayFailedMessages(List<MessageToReplay> messageToReplay) {
        logger.info("[MessageRecoverService] replaying messages");
        RecoverProperties.RabbitMQProp rabbitMqProp = recoverProperties.getRabbitMQProp();
        messageToReplay.forEach(
                message -> {
                    logger.info("[MessageRecoverService] replay message: {} with routing key: {}",
                            message.getRoutingKey(), message.getBodyToReplay());
                    rabbitMessagingTemplate.convertAndSend(
                            rabbitMqProp.getExchangeName(), message.getRoutingKey(), message.getBodyToReplay());
                }
        );
    }

    private List<MessageToReplay> parseFailedMessages(String messageBody) {
        if (messageBody == null) {
            return Collections.emptyList();
        }
        String lineSeparator = MessageFilter.LINE_SEPARATOR;
        List<String> messages = Arrays.asList(messageBody.split(
                URLEncoder.encode(lineSeparator + MessageFilter.MESSAGE_SEPARATOR + lineSeparator)));
        return messages.stream()
                .map(this::buildFailedMessage)
                .collect(Collectors.toList());
    }

    private MessageToReplay buildFailedMessage(String message) {
        String[] tempList = message.split("\n");
        String messageProperties = tempList[0].substring(tempList[0].indexOf(":{") + 1);
        MessageProperties messagePropertiesJson = convertStringToJSON(messageProperties);
        return new MessageToReplay(messagePropertiesJson.getRoutingKey(), tempList[1]);
    }

    private MessageProperties convertStringToJSON(String toJson) {
        ObjectMapper mapper = new ObjectMapper();

        MessageProperties messageProperties;
        try {
            messageProperties = mapper.readValue(toJson, MessageProperties.class);
        } catch (IOException e) {
            String errorMessage =
                    String.format("Error happened converting the message to a JSON string: %s", e.getMessage());
            logger.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        return messageProperties;
    }
}
