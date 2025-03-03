package io.jaconi.spring.rabbitmq.retry;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RetryErrorHandlerTest extends RabbitMQTest {

    @Test
    void handleError1() {
        var payload = "Test Message 1";

        // Send a message. Our test application will always throw a retry exception.
        amqpTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, payload);

        // Make sure the message is sent to the first retry queue within 5 seconds.
        var msg = amqpTemplate.receiveAndConvert("%s-retry-in-PT5S".formatted(QUEUE), Duration.ofSeconds(5).toMillis());
        assertThat(msg).isEqualTo(payload);
    }

    @Test
    void handleError2() {
        var payload = "Test Message 2";

        // Send a message. Our test application will always throw a retry exception.
        amqpTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, payload);

        // Make sure the message is sent to the second retry queue within 10 seconds.
        var msg = amqpTemplate.receiveAndConvert("%s-retry-in-PT10S".formatted(QUEUE), Duration.ofSeconds(10).toMillis());
        assertThat(msg).isEqualTo(payload);
    }

    @Test
    void handleError3() {
        var payload = "Test Message 3";

        // Send a message. Our test application will always throw a retry exception.
        amqpTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, payload);

        // Make sure the message is sent to the second retry queue within 20 seconds.
        var msg = amqpTemplate.receiveAndConvert("%s-retry-never".formatted(QUEUE), Duration.ofSeconds(20).toMillis());
        assertThat(msg).isEqualTo(payload);
    }

    @SuppressWarnings("unused")
    public static class TestApplication extends RabbitMQTest.RabbitMQTestApplication {

        @RabbitListener(queues = QUEUE)
        public void handle(Message message) {
            throw new RetryMessagesException(message);
        }
    }
}
