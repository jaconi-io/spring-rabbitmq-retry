package io.jaconi.spring.rabbitmq.retry;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RetryPropertiesTest {

    @Test
    void defaults() {
        var properties = new RetryProperties(false, null);
        assertNotNull(properties.exchanges());
        assertTrue(properties.exchanges().isEmpty());

        var exchangeProperties = new RetryProperties.RetryExchangeProperties(Collections.singletonList(Duration.of(10L, ChronoUnit.SECONDS)), null);
        assertNull(exchangeProperties.maxRetries());
    }

    @Test
    void validations() {
        assertThrows(IllegalArgumentException.class, () -> new RetryProperties.RetryExchangeProperties(null, null));
        assertThrows(IllegalArgumentException.class, () -> new RetryProperties.RetryExchangeProperties(Collections.emptyList(), null));
        assertThrows(IllegalArgumentException.class, () -> new RetryProperties.RetryExchangeProperties(Collections.singletonList(Duration.of(10L, ChronoUnit.SECONDS)), -1));
    }
}
