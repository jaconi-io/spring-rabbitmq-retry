package io.jaconi.spring.rabbitmq.retry;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RetryServiceTest extends RabbitMQTest {

    @Test
    void testFirstRetry() {
        var payload = "Test Message 1";

        // Send a message. Our test application will always throw a retry exception.
        amqpTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, payload);

        // Make sure the message is sent to the first retry queue within 5 seconds.
        var msg = amqpTemplate.receiveAndConvert("%s-retry-in-PT5S".formatted(QUEUE), Duration.ofSeconds(5).toMillis());
        assertThat(msg).isEqualTo(payload);
    }

    @Test
    void testSecondRetry() {
        var payload = "Test Message 2";

        // Send a message. Our test application will always throw a retry exception.
        amqpTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, payload);

        // Make sure the message is sent to the second retry queue within 10 seconds.
        var msg = amqpTemplate.receiveAndConvert("%s-retry-in-PT10S".formatted(QUEUE), Duration.ofSeconds(10).toMillis());
        assertThat(msg).isEqualTo(payload);
    }

    @Test
    void testThirdRetry() {
        var payload = "Test Message 3";

        // Send a message. Our test application will always throw a retry exception.
        amqpTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, payload);

        // Make sure the message is sent to the second retry queue within 20 seconds.
        var msg = amqpTemplate.receiveAndConvert("%s-retry-never".formatted(QUEUE), Duration.ofSeconds(20).toMillis());
        assertThat(msg).isEqualTo(payload);
    }

    @SuppressWarnings("unused")
    public static class TestApplication extends RabbitMQTestApplication {

        @Autowired
        @SuppressWarnings("unused")
        private RetryService retryService;

        @RabbitListener(queues = QUEUE)
        public void handle(Message message, Channel channel) throws IOException {
            retryService.retryMessage(message);

            if (channel != null) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            }
        }
    }
}
