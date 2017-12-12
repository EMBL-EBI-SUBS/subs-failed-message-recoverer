package uk.ac.ebi.subs.messagerecover.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Reading property values from application.yml file.
 */
@Component
@EnableConfigurationProperties
@ConfigurationProperties("messageRecover")
@Data
public class RecoverProperties {

    private String inputBindingRemovalDelayInSec;
    private RabbitMQProp rabbitMQProp;
    private QdbProp qdbProp;

    @Data
    public static class RabbitMQProp {
        private String baseURL;
        private String username;
        private String password;
        private String vhost;
        private String exchangeName;
        private String deadLetterExchangeName;
        private String deadLetterQueueName;
    }

    @Data
    public static class QdbProp {
        private String baseURL;
        private Queue queue;
        private MessageFilter messageFilter;

        @Data
        public static class Queue {
            private String deadLetterQueueName;
            private String basePath;
            private String inputBasePath;
            private String inputPath;
            private String outputBasePath;
            private String outputPath;
            private String maxSize;
            private String maxPayloadSize;
            private String contentType;
        }

        @Data
        public static class MessageFilter {
            private String grep;
            private String from;
            private String to;
            private String routingKey;
            private String fromId;
        }
    }
}