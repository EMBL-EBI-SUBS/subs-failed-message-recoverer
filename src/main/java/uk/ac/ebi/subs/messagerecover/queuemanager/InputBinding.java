package uk.ac.ebi.subs.messagerecover.queuemanager;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.ac.ebi.subs.messagerecover.config.RecoverProperties;

/**
 * Input binding configuration between RabbitMQ and QDB.
 */
@ToString
@Getter
@NoArgsConstructor
public class InputBinding {

    private String type = "rabbitmq";
    private String url;
    private String queue;

    public InputBinding(RecoverProperties.RabbitMQProp rabbitProp) {
        this.url = rabbitProp.getBaseURL();
        this.queue = rabbitProp.getDeadLetterQueueName();
    }
}
