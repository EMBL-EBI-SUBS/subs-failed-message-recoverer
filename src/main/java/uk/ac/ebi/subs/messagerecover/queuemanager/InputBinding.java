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
        String username = rabbitProp.getUsername().equals("") ? "" : rabbitProp.getUsername();
        String password = rabbitProp.getPassword().equals("") ? "" : ":" + rabbitProp.getPassword() + "@";
        String vhost = rabbitProp.getVhost().equals("") ? "" : "/" + rabbitProp.getVhost();

        this.url = "amqp://" + username + password + rabbitProp.getBaseURL() + vhost;
        this.queue = rabbitProp.getDeadLetterQueueName();
    }
}
