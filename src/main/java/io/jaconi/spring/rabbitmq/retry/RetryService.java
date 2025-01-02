package io.jaconi.spring.rabbitmq.retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.MessagingMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Retry AMQP messages as configured in the {@link RetryProperties}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryService {
    private static final MessagingMessageConverter CONVERTER = new MessagingMessageConverter();

    private final AmqpTemplate amqpTemplate;

    /**
     * Retry a {@link org.springframework.amqp.core.Message} by increasing the retry attempt in the message header and
     * sending the message to the retry exchange.
     *
     * @param message the {@link org.springframework.amqp.core.Message}
     */
    public void retryMessage(org.springframework.amqp.core.Message message) {
        retryMessage((Message<?>) CONVERTER.fromMessage(message));
    }

    /**
     * Retry a {@link Message} by increasing the retry attempt in the message header and sending the message to the
     * retry exchange.
     *
     * @param message the {@link Message}
     */
    public void retryMessage(Message<?> message) {
        var retry = getRetry(message);
        log.info("retrying message (attempt {}): {}", retry, message);

        var routingKey = message.getHeaders().get(AmqpHeaders.RECEIVED_ROUTING_KEY, String.class);
        amqpTemplate.convertAndSend(getRetryExchange(message), routingKey, message.getPayload(), m -> {
            m.getMessageProperties().setHeader(RetryProperties.RETRY_HEADER, retry);
            TechnicalHeadersFilter.filterHeaders(message.getHeaders())
                    .forEach(h -> m.getMessageProperties().setHeader(h, message.getHeaders().get(h)));
            return m;
        });
    }

    /**
     * Determine the retry attempt for the {@link Message}.
     *
     * @param message the {@link Message}
     * @return {@literal 1L} for the first retry, {@literal 2L} for the second, and so on
     */
    private long getRetry(Message<?> message) {
        Long previousRetryAttempt = message.getHeaders().get(RetryProperties.RETRY_HEADER, Long.class);
        if (previousRetryAttempt == null) {
            previousRetryAttempt = 0L;
        }

        return previousRetryAttempt + 1;
    }

    /**
     * Determine the retry exchange for a {@link Message}.
     *
     * @param message the {@link Message}
     * @return the retry exchange
     */
    private String getRetryExchange(Message<?> message) {
        return RetryProperties.RETRY_EXCHANGE_PATTERN.formatted(message.getHeaders().get(AmqpHeaders.CONSUMER_QUEUE, String.class));
    }
}
