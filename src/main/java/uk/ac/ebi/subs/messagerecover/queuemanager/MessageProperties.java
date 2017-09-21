package uk.ac.ebi.subs.messagerecover.queuemanager;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
public class MessageProperties {
    private int id;
    private String timestamp;
    private int payloadSize;
    private String routingKey;
}
