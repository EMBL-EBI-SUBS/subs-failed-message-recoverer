package uk.ac.ebi.subs.messagerecover;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.IOException;

@Data
public class TestMessage {

    private int id;
    private String message;
    private int testId;
    private String testMessage;

    public TestMessage() {
    }

    private static ObjectMapper objectMapper = new ObjectMapper();

    public TestMessage(String jsonString) throws IOException {
        TestMessage temp = objectMapper.readValue(jsonString, TestMessage.class);
        this.id = temp.id;
        this.message = temp.message;
        this.testId = temp.testId;
        this.testMessage = temp.testMessage;
    }
}
