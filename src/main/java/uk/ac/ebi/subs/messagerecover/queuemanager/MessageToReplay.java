package uk.ac.ebi.subs.messagerecover.queuemanager;

import lombok.Data;

/**
 * This entity holds a failed message, its routing key and its corrected message or the same message,
 * if the message was correct, but it failed because some service was down.
 */
@Data
public class MessageToReplay {

    private String routingKey;
    private String body;
    private String bodyToReplay;

    public MessageToReplay(String routingKey, String body) {
        this.routingKey = routingKey;
        this.body = body;
    }
}
