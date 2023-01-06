package io.jaconi.spring.rabbitmq.retry;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.messaging.MessageHeaders;

public class TechnicalHeadersFilter {

    private static final Set<String> EXCLUDE_HEADERS = Set.of(MessageHeaders.CONTENT_TYPE, MessageHeaders.ID,
            MessageHeaders.TIMESTAMP, MessageHeaders.ERROR_CHANNEL, MessageHeaders.REPLY_CHANNEL, DefaultClassMapper.DEFAULT_CLASSID_FIELD_NAME);

    /**
     * Determine the headers to send to the retry exchange
     *
     * @param messageHeaders The headers of the incoming message.
     * @return The header names to keep.
     */
    public static Set<String> filterHeaders(MessageHeaders messageHeaders) {
        return messageHeaders
                .keySet()
                .stream()
                // Remove all headers starting with "x-" or "amqp_" as well as heaaers in EXCLUDE_HEADERS
                .filter(h -> !(h.startsWith("x-") || h.startsWith(AmqpHeaders.PREFIX) || EXCLUDE_HEADERS.contains(h)))
                .collect(Collectors.toSet());
    }
}
