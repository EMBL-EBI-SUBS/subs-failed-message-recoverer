package uk.ac.ebi.subs.messagerecover.queuemanager;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.messagerecover.config.RecoverProperties;

/**
 * Input binding configuration between RabbitMQ and QDB.
 */
@Data
@NoArgsConstructor
@Component
public class InputBinding {

    private String type = "rabbitmq";
    private String url;
    private String queue;

    public InputBinding(RecoverProperties.RabbitMQProp rabbitProp, String url) {
        this.queue = rabbitProp.getDeadLetterQueueName();
        this.url = url;
    }
}
