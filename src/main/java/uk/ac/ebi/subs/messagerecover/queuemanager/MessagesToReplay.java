package uk.ac.ebi.subs.messagerecover.queuemanager;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * This entity holds a failed message, its routing key and its corrected message or the same message,
 * if the message was correct, but it failed because some service was down.
 */
@ToString
@Getter
@Setter
public class MessagesToReplay {

    private String routingKey;
    private String body;
    private String bodyToReplay;

    public MessagesToReplay(String routingKey, String body) {
        this.routingKey = routingKey;
        this.body = body;
    }
}
