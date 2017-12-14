package uk.ac.ebi.subs.messagerecover.queuemanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.subs.messagerecover.config.RecoverProperties;

@Component
public class QDBManager {

    private static final Logger logger = LoggerFactory.getLogger(QDBManager.class);

    private RestTemplate restTemplate;

    private RecoverProperties recoverProperties;

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.virtual-host}")
    private String vhost;

    public QDBManager(RecoverProperties recoverProperties) {
        this.restTemplate = new RestTemplate();
        this.recoverProperties = recoverProperties;
    }

    public ResponseEntity<String> createQDBDeadLetterQueue(String queueName) {
        logger.info("[QDBManager] Creating a QDB queue: {}", queueName);
        return restTemplate.postForEntity(buildCreateQueueURL(queueName), null, String.class);
    }

    public void addInputBindingToQDBDeadLetterQueue(String rabbitQueueName, String qdbQueueName) {
        logger.info("[QDBManager] Creating an input binding between **{}** RabbitMQ queue and **{}** QDB queue",
                rabbitQueueName, qdbQueueName);
        RecoverProperties.RabbitMQProp rabbitProp = recoverProperties.getRabbitMQProp();

        InputBinding inputBinding = new InputBinding(rabbitProp, constructUrl());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<InputBinding> entity = new HttpEntity<>(inputBinding, headers);

        restTemplate.postForEntity(buildInputQueueBindingURL(qdbQueueName), entity, InputBinding.class);
    }

    public void removeInputBindingFromQDBDeadLetterQueue(String rabbitQueueName, String qdbQueueName) {
        logger.info("[QDBManager] Removing the input binding between **{}** RabbitMQ queue and **{}** QDB queue",
                rabbitQueueName, qdbQueueName);

        restTemplate.delete(buildInputQueueBindingURL(qdbQueueName));
    }

    public ResponseEntity<String> filterMessagesFromQDBDeadLetterQueue(String qdbQueueName) {
        MessageFilter messageFilter = new MessageFilter(recoverProperties.getQdbProp().getMessageFilter());
        logger.info("[QDBManager] Reading messages from the QDB Failure queue");
        logger.info("Applied filter: {}", messageFilter);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(buildGetMessagesURL(qdbQueueName))
                .query(messageFilter.toQueryString());

        return restTemplate.getForEntity(builder.toUriString(), String.class);
    }

    private String constructUrl() {
        return "amqp://" + username + ":" + password + "@" + host + "/" + vhost;
    }

    private String buildGetMessagesURL(String queueName) {
        return String.join("/", buildCreateQueueURL(queueName), "messages");
    }

    private String buildCreateQueueURL(String queueName) {
        RecoverProperties.QdbProp qdbProp = recoverProperties.getQdbProp();
        StringBuilder builder = new StringBuilder(qdbProp.getBaseURL());
        builder
                .append(qdbProp.getQueue().getBasePath())
                .append("/")
                .append(queueName);

        return builder.toString();
    }

    private String buildInputQueueBindingURL(String queueName) {
        RecoverProperties.QdbProp qdbProp = recoverProperties.getQdbProp();
        RecoverProperties.QdbProp.Queue queueProp = qdbProp.getQueue();
        return buildQueuePath(queueName)
                .append(queueProp.getInputBasePath())
                .append(queueProp.getInputPath()).toString();
    }

    private StringBuilder buildQueuePath(String queueName) {
        RecoverProperties.QdbProp qdbProp = recoverProperties.getQdbProp();
        RecoverProperties.QdbProp.Queue queueProp = qdbProp.getQueue();
        StringBuilder builder = new StringBuilder(qdbProp.getBaseURL());
        builder
                .append(queueProp.getBasePath())
                .append("/")
                .append(queueName);

        return builder;
    }
}
