package uk.ac.ebi.subs.messagerecover.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.messagerecover.config.RecoverProperties;
import uk.ac.ebi.subs.messagerecover.queuemanager.MessagesToReplay;
import uk.ac.ebi.subs.messagerecover.queuemanager.MessageFilter;
import uk.ac.ebi.subs.messagerecover.queuemanager.MessageProperties;
import uk.ac.ebi.subs.messagerecover.queuemanager.QDBManager;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

    public MessageRecoverService(RecoverProperties recoverProperties, QDBManager qdbManager,
                                 RabbitMessagingTemplate rabbitMessagingTemplate) {
        this.recoverProperties = recoverProperties;
        this.qdbManager = qdbManager;
        this.rabbitMessagingTemplate = rabbitMessagingTemplate;
    }

    /**
     * Creates a queue in QDB server for storing the failed messages.
     *
     * @return the name of the created QDB queue
     */
    public String createQDBDeadLetterQueue() {
        String qdbQueueName = recoverProperties.getQdbProp().getQueue().getDeadLetterQueueName();
        qdbManager.createQDBDeadLetterQueue(qdbQueueName);

        return qdbQueueName;
    }

    /**
     * Add a binding between a RabbitMQ and a QDB queue.
     *
     * @param qdbQueueName the name of the QDB queue to bind to the RabbitMQ queue.
     *                     The name of the RabbitMQ queue has been defined in the application.yml file.
     */
    public void addInputBindingToQDBDeadLetterQueue(String qdbQueueName) {
        String rabbitQueueName = recoverProperties.getRabbitMQProp().getDeadLetterQueueName();
        qdbManager.addInputBindingToQDBDeadLetterQueue(rabbitQueueName, qdbQueueName);
    }

    /**
     * Remove a binding between a RabbitMQ and a QDB queue.
     * Before removing the binding it waits a defined amount of time (configurable in the application.yml file).
     *
     * @param qdbQueueName the name of the QDB queue from remove the binding bind to the RabbitMQ queue.
     * @throws InterruptedException
     */
    public void removeInputBindingFromQDBDeadLetterQueue(String qdbQueueName) throws InterruptedException {
        Thread.sleep(1000 * Integer.valueOf(recoverProperties.getInputBindingRemovalDelayInSec()));

        String rabbitQueueName = recoverProperties.getRabbitMQProp().getDeadLetterQueueName();
        qdbManager.removeInputBindingFromQDBDeadLetterQueue(rabbitQueueName, qdbQueueName);
    }

    /**
     * Retrieves messages with applied filter criteria defined in the application.yml file.
     *
     * @param qdbQueueName the name of the QDB queue retrieve the message(s) from
     * @return a filtered {@link List} of {@link MessagesToReplay}
     */
    public List<MessagesToReplay> readFilterMessagesFromQDBDaedLetterQueue(String qdbQueueName) {
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
     * @param messagesToReplay a {@link List} of {@link MessagesToReplay} that holds the original failed messages
     *                         and their routing keys
     */
    public void fixFailedMessages(List<MessagesToReplay> messagesToReplay) {
        logger.info("[MessageRecoverService] fixing messages");

        messagesToReplay.forEach(message -> message.setBodyToReplay(message.getBody()));
    }

    /**
     * Replay the fixed/corrected messages with their original routing key to the given RabbitMQ exchange.
     * The exchange is defined in the application.yml configuration file.
     *
     * @param messagesToReplay a {@link List} of {@link MessagesToReplay}
     */
    public void replayFailedMessages(List<MessagesToReplay> messagesToReplay) {
        logger.info("[MessageRecoverService] replaying messages");
        RecoverProperties.RabbitMQProp rabbitMqProp = recoverProperties.getRabbitMQProp();
        messagesToReplay.forEach(
                message -> {
                    logger.info("[MessageRecoverService] replay message: {} with routing key: {}",
                            message.getRoutingKey(), message.getBodyToReplay());
                    rabbitMessagingTemplate.convertAndSend(
                            rabbitMqProp.getExchangeName(), message.getRoutingKey(), message.getBodyToReplay());
                }
        );
    }

    private List<MessagesToReplay> parseFailedMessages(String messageBody) {
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

    private MessagesToReplay buildFailedMessage(String message) {
        String[] tempList = message.split("\n");
        String messageProperties = tempList[0].substring(tempList[0].indexOf(":{") + 1);
        MessageProperties messagePropertiesJson = convertStringToJSON(messageProperties);
        return new MessagesToReplay(messagePropertiesJson.getRoutingKey(), tempList[1]);
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
