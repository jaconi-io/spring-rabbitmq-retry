package io.jaconi.spring.rabbitmq.retry;

import lombok.Getter;
import org.springframework.amqp.support.converter.MessagingMessageConverter;
import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Getter
public class RetryMessagesException extends RuntimeException {
    private static final MessagingMessageConverter CONVERTER = new MessagingMessageConverter();
    private final Collection<Message<?>> messages;

    public <T> RetryMessagesException(Message<T> message) {
        this(Collections.singleton(message));
    }

    @SuppressWarnings("unchecked")
    public <T> RetryMessagesException(org.springframework.amqp.core.Message message) {
        this((Message<T>) CONVERTER.fromMessage(message));
    }

    public <T> RetryMessagesException(String message, Message<T> message1) {
        this(message, Collections.singleton(message1));
    }

    @SuppressWarnings("unchecked")
    public <T> RetryMessagesException(String message, org.springframework.amqp.core.Message message1) {
        this(message, (Message<T>) CONVERTER.fromMessage(message1));
    }

    public <T> RetryMessagesException(Throwable cause, Message<T> message) {
        this(cause, Collections.singleton(message));
    }

    @SuppressWarnings("unchecked")
    public <T> RetryMessagesException(Throwable cause, org.springframework.amqp.core.Message message) {
        this(cause, (Message<T>) CONVERTER.fromMessage(message));
    }

    public <T> RetryMessagesException(String message, Throwable cause, Message<T> message1) {
        this(message, cause, Collections.singleton(message1));
    }

    @SuppressWarnings("unchecked")
    public <T> RetryMessagesException(String message, Throwable cause, org.springframework.amqp.core.Message message1) {
        this(message, cause, (Message<T>) CONVERTER.fromMessage(message1));
    }

    public <T> RetryMessagesException(Collection<Message<T>> messages) {
        super("retrying %d erroneous messages".formatted(messages.size()));
        this.messages = new ArrayList<>();
        this.messages.addAll(messages);
    }

    public <T> RetryMessagesException(String message, Collection<Message<T>> messages) {
        super(message);
        this.messages = new ArrayList<>();
        this.messages.addAll(messages);
    }

    public <T> RetryMessagesException(Throwable cause, Collection<Message<T>> messages) {
        super("retrying %d erroneous messages".formatted(messages.size()), cause);
        this.messages = new ArrayList<>();
        this.messages.addAll(messages);
    }

    public <T> RetryMessagesException(String message, Throwable cause, Collection<Message<T>> messages) {
        super(message, cause);
        this.messages = new ArrayList<>();
        this.messages.addAll(messages);
    }
}
