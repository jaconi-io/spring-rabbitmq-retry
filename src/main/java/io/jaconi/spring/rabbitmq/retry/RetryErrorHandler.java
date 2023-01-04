package io.jaconi.spring.rabbitmq.retry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.ImmediateAcknowledgeAmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Custom error handling for RabbitMQ. By default, Spring AMQP retries most exceptions by immediately requeuing the
 * causing message. This error handler rejects any messages causing an exception in the listener. If messages should be
 * retried, the listener code can throw a {@link RetryMessagesException}. The messages in the
 * {@link RetryMessagesException} are retried as configured in the {@link RetryProperties}.
 */
@Slf4j
@Component("retryErrorHandler")
public class RetryErrorHandler extends ConditionalRejectingErrorHandler {
    private final AmqpTemplate amqpTemplate;

    public RetryErrorHandler(AmqpTemplate amqpTemplate) {
        super(new DefaultExceptionStrategy() {
            @Override
            protected boolean isUserCauseFatal(@NonNull Throwable cause) {
                // Do not requeue anything, even if default-requeue-rejected is true.
                return true;
            }
        });

        this.amqpTemplate = amqpTemplate;
    }

    @Override
    public void handleError(@NonNull Throwable t) {
        if (t instanceof ListenerExecutionFailedException lefe && lefe.getCause() instanceof RetryMessagesException rme) {
            rme.getMessages().forEach(this::retryMessage);
            throw new ImmediateAcknowledgeAmqpException("acknowledge messages as they were scheduled for retry", t);
        } else {
            super.handleError(t);
        }
    }

    private void retryMessage(Message<?> message) {
        var retry = getRetry(message);
        log.info("retrying message (attempt {}): {}", retry, message);

        var routingKey = message.getHeaders().get("amqp_receivedRoutingKey", String.class);
        amqpTemplate.convertAndSend(getRetryExchange(message), routingKey, message.getPayload(), m -> {
            m.getMessageProperties().setHeader(RetryProperties.RETRY_HEADER, retry);
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
        return RetryProperties.RETRY_EXCHANGE_PATTERN.formatted(message.getHeaders().get("amqp_receivedExchange", String.class));
    }
}
