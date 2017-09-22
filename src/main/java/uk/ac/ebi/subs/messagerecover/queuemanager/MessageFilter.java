package uk.ac.ebi.subs.messagerecover.queuemanager;

import lombok.Data;
import uk.ac.ebi.subs.messagerecover.config.RecoverProperties;

import java.net.URLEncoder;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is responsible to create q query string for retrieve filtered messages from a given QDB queue.
 * The query string parameters is coming from the application.yml file.
 */
@Data
public class MessageFilter {

    public static final String MESSAGE_SEPARATOR = "########";
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final String QUERY_STRING_STARTER = "?";
    public static final String QUERY_STRING_SEPARATOR = "&";

    private String grep;
    private String from;
    private String to;
    private String routingKey;
    private String fromId = "1";
    private String timeoutMs = "1";

    public MessageFilter(RecoverProperties.QdbProp.MessageFilter messageFilter) {
        this.grep = messageFilter.getGrep();
        this.from = messageFilter.getFrom();
        this.to = messageFilter.getTo();
        this.routingKey = messageFilter.getRoutingKey();
    }

    /**
     * Returns a query string from the parameters set in the application.yml file.
     * If the query string is not empty, then it will append some additional necessary parameters to the string.
     * @return
     */
    String toQueryString() {
        String queryString = addDefinedQueryStringParameters();

        return queryString.length() > 0 ? queryString + QUERY_STRING_SEPARATOR + addAdditionalQueryString() + addMessageSeparator() : "";
    }

    private String addDefinedQueryStringParameters() {
        return addToQueryString(Arrays.asList(
                new SimpleEntry<>("grep", grep),
                new SimpleEntry<>("from", from),
                new SimpleEntry<>("to", to),
                new SimpleEntry<>("routingKey", routingKey)
                )
        );
    }

    /**
     * If the calculated query string is not empty, then we need to add the 'fromId=1' and 'timeouts=1' parameters
     * to the query string to query the messages from the beginning and we also have to set the timeout value.
     *
     * @return additional parameters for the calculated query string.
     */
    String addAdditionalQueryString() {
        return addToQueryString(Arrays.asList(
                new SimpleEntry<>("fromId", fromId),
                new SimpleEntry<>("timeoutMs", timeoutMs)
                )
        );
    }

    private String addToQueryString(List<SimpleEntry<String, String>> queryStringParams) {
        return queryStringParams.stream()
                .filter( param -> param.getValue() != null && !param.getValue().equals(""))
                .map(param -> param.getKey() + "=" + URLEncoder.encode(param.getValue()))
                .collect(Collectors.joining(QUERY_STRING_SEPARATOR));
    }

    /**
     * Returns a separator to put between messages to be able to parse it more easier.
     *
     * @return a separator to put between messages to be able to parse it more easier.
     */
    String addMessageSeparator() {
        return QUERY_STRING_SEPARATOR + "separator=" + LINE_SEPARATOR + MESSAGE_SEPARATOR + LINE_SEPARATOR;
    }
}
