package uk.ac.ebi.subs.messagerecover.queuemanager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.messagerecover.config.RecoverProperties;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
public class MessageFilterTest {

    private final String TEST_ROUTING_KEY = "this.is.a.test.routingKey";
    private final String TEST_GREP = "sampleid_123";

    RecoverProperties.QdbProp.MessageFilter messageFilterProperties;

    @Before
    public void setup() {
        messageFilterProperties = new RecoverProperties.QdbProp.MessageFilter();
    }

    @Test
    public void whenPassingAValidMessageFilterWithRoutingKeyFilled_ThenGetQueryStringWithRoutingKey() {
        messageFilterProperties.setRoutingKey(TEST_ROUTING_KEY);
        MessageFilter messageFilter = new MessageFilter(messageFilterProperties);

        final String EXPECTED_QUERY_STRING = "routingKey=" + TEST_ROUTING_KEY + MessageFilter.QUERY_STRING_SEPARATOR +
                messageFilter.addAdditionalQueryString() + messageFilter.addMessageSeparator();

        assertThat(messageFilter.toQueryString(), is(equalTo(EXPECTED_QUERY_STRING)));
    }

    @Test
    public void whenPassingAValidMessageFilterWithRoutingKeyAndGrepFilled_ThenGetQueryStringWithRoutingKeyAndGrep() {
        messageFilterProperties.setRoutingKey(TEST_ROUTING_KEY);
        messageFilterProperties.setGrep(TEST_GREP);
        MessageFilter messageFilter = new MessageFilter(messageFilterProperties);

        final String EXPECTED_QUERY_STRING =
                "grep=" + TEST_GREP + MessageFilter.QUERY_STRING_SEPARATOR +
                "routingKey=" + TEST_ROUTING_KEY + MessageFilter.QUERY_STRING_SEPARATOR +
                messageFilter.addAdditionalQueryString() + messageFilter.addMessageSeparator();

        assertThat(messageFilter.toQueryString(), is(equalTo(EXPECTED_QUERY_STRING)));

    }

    @Test
    public void whenPassingAMessageFilterWithoutParams_ThenGetAnEmptyQueryString() {
        MessageFilter messageFilter = new MessageFilter(messageFilterProperties);

        final String EXPECTED_QUERY_STRING = "";

        assertThat(messageFilter.toQueryString(), is(equalTo(EXPECTED_QUERY_STRING)));
    }
}
