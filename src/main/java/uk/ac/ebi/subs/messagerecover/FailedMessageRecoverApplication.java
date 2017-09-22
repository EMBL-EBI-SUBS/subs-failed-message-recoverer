package uk.ac.ebi.subs.messagerecover;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import uk.ac.ebi.subs.messagerecover.queuemanager.MessagesToReplay;
import uk.ac.ebi.subs.messagerecover.service.MessageRecoverService;

import java.util.List;

@SpringBootApplication
@ComponentScan("uk.ac.ebi.subs.messagerecover")
public class FailedMessageRecoverApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(FailedMessageRecoverApplication.class);

    @Autowired
    private MessageRecoverService recovererService;

    public static void main(String[] args) {
        SpringApplication.run(FailedMessageRecoverApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Application has started");

        String qdbQueueName = recovererService.createQDBDeadLetterQueue();
        recovererService.addInputBindingToQDBDeadLetterQueue(qdbQueueName);

        // wait for message transfering from RabbitMQ, then removing the input bindings
        // to avoiding creating a circular message routing
        recovererService.removeInputBindingFromQDBDeadLetterQueue(qdbQueueName);

        List<MessagesToReplay> messagesToReplay = recovererService.readFilterMessagesFromQDBDaedLetterQueue(qdbQueueName);
        logger.info("Messages from QDB queue: {}", messagesToReplay);

        if (messagesToReplay.size() > 0) {
            recovererService.fixFailedMessages(messagesToReplay);

            recovererService.replayFailedMessages(messagesToReplay);
        } else {
            logger.info("No messages to replay");
        }

        logger.info("Application has finished");
    }
}
