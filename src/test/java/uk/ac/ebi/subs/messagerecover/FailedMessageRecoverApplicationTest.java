package uk.ac.ebi.subs.messagerecover;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@Profile("test")
public class FailedMessageRecoverApplicationTest {

    public static void main(String[] args) {
        SpringApplication.run(FailedMessageRecoverApplicationTest.class, args);
    }
}
