package uk.ac.ebi.subs.messagerecover;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.messagerecover.config.RecoverProperties;
import uk.ac.ebi.subs.messagerecover.queuemanager.MessageToReplay;
import uk.ac.ebi.subs.messagerecover.service.MessageRecoverService;
import uk.ac.ebi.subs.messagerecover.util.RabbitMQAndQDBDependentTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FailedMessageRecoverTestApplication.class)
@Category(RabbitMQAndQDBDependentTest.class)
public class RecoveryIntegrationTest {

    private static Logger logger = LoggerFactory.getLogger(RecoveryIntegrationTest.class);

    private static final String TEST_MESSAGE = "Added test message";
    private static final int TEST_ID = 123;
    private static final String DEFAULT_TEST_MESSAGE = "This is a test message ";
    private static final int FAILED_MESSAGE_COUNT = 10;

    private List<Message> replayedMessages = new ArrayList<>();
    private List<String> testRoutingKeys = new ArrayList<>();

    @Autowired
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    @Autowired
    private RecoverProperties recoverProperties;

    @SpyBean
    private MessageRecoverService recoverService;

    private List<String> routingKeys;

    @Before
    public void setupBeforeClass() {
        testRoutingKeys = Arrays.asList("test.sample.routingKey", "test.study.routingKey", "another.sample.routingKey");

        rabbitMessagingTemplate.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    public void testMessageReplaying() throws InterruptedException {
        publishMessages();
        recoverService.transferMessagesToQDBDeadLetterQueue();
        List<MessageToReplay> messagesToReplay = recoverService.readFilterMessagesFromQDBDaedLetterQueue();
        System.out.println(messagesToReplay);

        doAnswer(RecoveryIntegrationTest::repairMessages).when(recoverService).fixFailedMessages(messagesToReplay);

        recoverService.fixFailedMessages(messagesToReplay);
        System.out.println(messagesToReplay);

        recoverService.replayFailedMessages(messagesToReplay);

        // Waiting for consuming the republished messages for check the result
        Thread.sleep(3 * 1000);

        checkReplayedMessagesCorrectness();
    }

    private void publishMessages() {
        List<TestMessage> messages = generateMessages();
        routingKeys = generateRoutingKeys();
        for (int i = 0; i < FAILED_MESSAGE_COUNT; i++) {
            rabbitMessagingTemplate.convertAndSend(
                    "usi-1:dead-letter-exchange", routingKeys.get(i), messages.get(i));
        }
    }

    private static Object repairMessages(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        List<MessageToReplay> messagesToFix = (List<MessageToReplay>) args[0];
        messagesToFix.forEach(messageToFix -> {
            TestMessage testMessage = convertStringToObject(messageToFix.getBody());
            testMessage.setTestMessage(TEST_MESSAGE);
            testMessage.setTestId(TEST_ID);
            convertObjectToString(messageToFix, testMessage);
        });
        return messagesToFix;
    }

    private List<TestMessage> generateMessages() {
        List<TestMessage> testMessages = new ArrayList<>();
        for (int i = 0; i < FAILED_MESSAGE_COUNT; i++) {
            String message = DEFAULT_TEST_MESSAGE + i;
            TestMessage testMessage = new TestMessage();
            testMessage.setTestId(i);
            testMessage.setMessage(message);
            testMessages.add(testMessage);
        }

        return testMessages;
    }

    private List<String> generateRoutingKeys() {
        List<String> routingKeys = new ArrayList<>();
        int routingKeyTypeCount = testRoutingKeys.size();
        for (int i = 0; i < FAILED_MESSAGE_COUNT; i++) {
            routingKeys.add(testRoutingKeys.get(i % routingKeyTypeCount));
        }

        return routingKeys;
    }

    private void checkReplayedMessagesCorrectness() {
        final String testRoutingKey = recoverProperties.getQdbProp().getMessageFilter().getRoutingKey();
        int matchingRoutingKeyCount = new Long(routingKeys.stream()
                .filter(routingKey -> routingKey.equals(testRoutingKey))
                .count()).intValue();
        assertThat(replayedMessages.size(), is(equalTo(matchingRoutingKeyCount)));

        replayedMessages.forEach(replayedMessage -> {
            TestMessage testMessage = convertStringToObject(new String(replayedMessage.getBody()));
            assertNotNull(testMessage.getMessage());
            assertThat(testMessage.getMessage(), is(containsString(DEFAULT_TEST_MESSAGE)));
            assertThat(testMessage.getTestMessage(), is(equalTo(TEST_MESSAGE)));
            assertThat(testMessage.getTestId(), is(equalTo(TEST_ID)));

            assertThat(replayedMessage.getMessageProperties().getReceivedRoutingKey(), is(equalTo(testRoutingKey)));
        });
    }

    private static TestMessage convertStringToObject(String message) {
        ObjectMapper mapper = new ObjectMapper();
        TestMessage testMessage;
        try {
            testMessage = mapper.readValue(message, TestMessage.class);
        } catch (IOException e) {
            return logAndThrowJSONConversionError(e);
        }

        return testMessage;
    }

    private static TestMessage convertObjectToString(MessageToReplay messageToFix, TestMessage testMessage) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            messageToFix.setBodyToReplay(mapper.writeValueAsString(testMessage));
        } catch (IOException e) {
            return logAndThrowJSONConversionError(e);
        }

        return testMessage;
    }

    private static TestMessage logAndThrowJSONConversionError(IOException e) {
        String errorMessage =
                String.format("Error happened converting the message to an Object: %s", e.getMessage());
        logger.error(errorMessage);
        throw new RuntimeException(errorMessage);
    }


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${messageRecover.rabbitMQProp.replayQueueName}", durable = "true", autoDelete = "true"),
            exchange = @Exchange(value = "${messageRecover.rabbitMQProp.exchangeName}", durable = "true", type = ExchangeTypes.TOPIC),
            key = "${messageRecover.qdbProp.messageFilter.routingKey}")
    )
    private void consumeReplayedMessages(Message replayedMessage) {
        replayedMessages.add(replayedMessage);
    }
}
