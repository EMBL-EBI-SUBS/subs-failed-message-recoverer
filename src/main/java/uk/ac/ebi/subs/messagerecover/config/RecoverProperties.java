package uk.ac.ebi.subs.messagerecover.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Reading property values from application.yml file.
 */
@Component
@EnableConfigurationProperties
@ConfigurationProperties("messageRecover")
@ToString
@Getter
@Setter
public class RecoverProperties {

    private String inputBindingRemovalDelayInSec;
    private RabbitMQProp rabbitMQProp;
    private QdbProp qdbProp;

    @ToString
    @Getter
    @Setter
    public static class RabbitMQProp {
        private String baseURL;
        private String exchangeName;
        private String deadLetterExchangeName;
        private String deadLetterQueueName;
    }

    @ToString
    @Getter
    @Setter
    public static class QdbProp {
        private String baseURL;
        private Queue queue;
        private MessageFilter messageFilter;

        @ToString
        @Getter
        @Setter
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

        @ToString
        @Getter
        @Setter
        public static class MessageFilter {
            private String grep;
            private String from;
            private String to;
            private String routingKey;
            private String fromId;
        }
    }
}