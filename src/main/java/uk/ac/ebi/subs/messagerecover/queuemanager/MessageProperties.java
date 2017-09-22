package uk.ac.ebi.subs.messagerecover.queuemanager;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MessageProperties {
    private int id;
    private String timestamp;
    private int payloadSize;
    private String routingKey;
}
