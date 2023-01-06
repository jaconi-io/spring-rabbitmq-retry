package io.jaconi.spring.rabbitmq.retry;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicalHeadersFilterTest {

    @Test
    void filterEmpty() {
        var source = new MessageHeaders(Map.of());

        var remaining = TechnicalHeadersFilter.filterHeaders(source);

        assertThat(remaining).isEmpty();
    }

    @Test
    void filterKeepOriginal() {
        var source = new MessageHeaders(Map.of(
                "user_id", "elliot",
                "tenant_id", "evilcorp",
                "amqp_consumerQueue", "event",
                "x-death-count", "3",
                "timestamp", "now"));

        var remaining = TechnicalHeadersFilter.filterHeaders(source);

        assertThat(remaining).containsExactlyInAnyOrder("user_id", "tenant_id");
    }
}