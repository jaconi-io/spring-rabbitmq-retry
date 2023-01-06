package io.jaconi.spring.rabbitmq.retry;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryPropertiesTest {

    @Test
    void defaults() {
        var properties = new RetryProperties(false, null);
        assertNotNull(properties.queues());
        assertTrue(properties.queues().isEmpty());

        var exchangeProperties = new RetryProperties.RetryQueueProperties(Collections.singletonList(Duration.of(10L, ChronoUnit.SECONDS)), null);
        assertNull(exchangeProperties.maxAttempts());
    }

    @Test
    void validations() {
        assertThrows(IllegalArgumentException.class, () -> new RetryProperties.RetryQueueProperties(null, null));
        assertThrows(IllegalArgumentException.class, () -> new RetryProperties.RetryQueueProperties(Collections.emptyList(), null));
        assertThrows(IllegalArgumentException.class, () -> new RetryProperties.RetryQueueProperties(Collections.singletonList(Duration.of(10L, ChronoUnit.SECONDS)), -1));
    }
}
