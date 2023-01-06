package io.jaconi.spring.rabbitmq.retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "jaconi.rabbitmq.listener.retry")
public record RetryProperties(boolean enabled, Map<String, RetryQueueProperties> queues) {
    static final String DEAD_LETTER_EXCHANGE_PATTERN = "%s-retry-never";
    static final String DEAD_LETTER_QUEUE_PATTERN = "%s-retry-never";
    static final String RETRY_EXCHANGE_PATTERN = "%s-retry";

    static final String DISPATCH_EXCHANGE_PATTERN = "%s-dispatch";
    static final String RETRY_QUEUE_PATTERN = "%s-retry-in-%s";
    static final String RETRY_HEADER = "x-retry";

    public RetryProperties {
        if (queues == null) {
            queues = Collections.emptyMap();
        }
    }

    public record RetryQueueProperties(List<Duration> durations, Integer maxRetries) {
        public RetryQueueProperties {
            Assert.notNull(durations, "durations are missing");
            Assert.notEmpty(durations, "at least one duration is required");
            Assert.isTrue(maxRetries == null || maxRetries >= 0, "max-retries must be greater than or equal to zero");
        }
    }
}
