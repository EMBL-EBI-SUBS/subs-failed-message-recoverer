package uk.ac.ebi.subs.messagerecover.queuemanager;

import lombok.Data;
import lombok.NoArgsConstructor;
import uk.ac.ebi.subs.messagerecover.config.RecoverProperties;

/**
 * Input binding configuration between RabbitMQ and QDB.
 */
@Data
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
