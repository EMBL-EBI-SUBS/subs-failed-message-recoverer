package uk.ac.ebi.subs.messagerecover;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import uk.ac.ebi.subs.messagerecover.queuemanager.MessageToReplay;
import uk.ac.ebi.subs.messagerecover.service.MessageRecoverService;

import java.util.List;

@SpringBootApplication
public class FailedMessageRecoverApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(FailedMessageRecoverApplication.class);

    private MessageRecoverService recoverService;

    public FailedMessageRecoverApplication(MessageRecoverService recoverService) {
        this.recoverService = recoverService;
    }

    public static void main(String[] args) {
        SpringApplication.run(FailedMessageRecoverApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Application has started");

        recoverService.transferMessagesToQDBDeadLetterQueue();

        List<MessageToReplay> messagesToReplay = recoverService.readFilterMessagesFromQDBDaedLetterQueue();
        logger.info("Messages from QDB queue: {}", messagesToReplay);

        if (messagesToReplay.size() > 0) {
            recoverService.fixFailedMessages(messagesToReplay);

            recoverService.replayFailedMessages(messagesToReplay);
        } else {
            logger.info("No messages to replay");
        }

        logger.info("Application has finished");
    }
}
