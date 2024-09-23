package io.jaconi.spring.rabbitmq.retry;

import org.springframework.amqp.ImmediateAcknowledgeAmqpException;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Custom error handling for RabbitMQ. By default, Spring AMQP retries most exceptions by immediately re-queuing the
 * causing message. This error handler rejects any messages causing an exception in the listener. If messages should be
 * retried, the listener code can throw a {@link RetryMessagesException}. The messages in the
 * {@link RetryMessagesException} are retried as configured in the {@link RetryProperties}.
 */
@Component("retryErrorHandler")
public class RetryErrorHandler extends ConditionalRejectingErrorHandler {
    private final RetryService retryService;

    public RetryErrorHandler(RetryService retryService) {
        super(new DefaultExceptionStrategy() {
            @Override
            protected boolean isUserCauseFatal(@NonNull Throwable cause) {
                // Do not requeue anything, even if default-requeue-rejected is true.
                return true;
            }
        });

        this.retryService = retryService;
    }

    @Override
    public void handleError(@NonNull Throwable t) {
        if (t instanceof ListenerExecutionFailedException lefe && lefe.getCause() instanceof RetryMessagesException rme) {
            rme.getMessages().forEach(retryService::retryMessage);
            throw new ImmediateAcknowledgeAmqpException("acknowledge messages as they were scheduled for retry", t);
        } else {
            super.handleError(t);
        }
    }
}
