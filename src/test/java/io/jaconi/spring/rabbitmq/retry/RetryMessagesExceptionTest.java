package io.jaconi.spring.rabbitmq.retry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetryMessagesExceptionTest {

    @Test
    void messageConversion() {
        var msg = new org.springframework.amqp.core.Message(new byte[]{'{', '}'});
        msg.getMessageProperties().setHeader(RetryProperties.RETRY_HEADER, 1L);

        var e = new RetryMessagesException(msg);

        assertEquals(1, e.getMessages().size());
        var converted = e.getMessages().iterator().next();

        assertNotNull(converted.getPayload());
        assertEquals(1L, converted.getHeaders().get(RetryProperties.RETRY_HEADER, Long.class));
    }
}
