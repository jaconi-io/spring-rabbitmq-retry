package io.jaconi.spring.rabbitmq.retry;

import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class RetryMessagesException extends RuntimeException {
    private final Collection<Message<?>> messages;

    public <T> RetryMessagesException(Message<T> message) {
        this(Collections.singleton(message));
    }

    public <T> RetryMessagesException(String message, Message<T> message1) {
        this(message, Collections.singleton(message1));
    }

    public <T> RetryMessagesException(Throwable cause, Message<T> message) {
        this(cause, Collections.singleton(message));
    }

    public <T> RetryMessagesException(String message, Throwable cause, Message<T> message1) {
        this(message, cause, Collections.singleton(message1));
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

    public Collection<Message<?>> getMessages() {
        return messages;
    }
}
