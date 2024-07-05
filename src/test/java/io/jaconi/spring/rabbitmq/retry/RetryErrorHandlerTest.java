package io.jaconi.spring.rabbitmq.retry;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jaconi.rabbitmq.listener.retry.enabled=true",
        "jaconi.rabbitmq.listener.retry.create-resources=true"
})
class RetryErrorHandlerTest {
    public static final String EXCHANGE = "test-exchange";
    public static final String QUEUE = "test-queue";
    public static final String ROUTING_KEY = "foo";

    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    @SuppressWarnings("unused")
    private AmqpTemplate amqpTemplate;

    @BeforeAll
    static void beforeAll() {
        rabbit.start();
    }

    @AfterAll
    static void afterAll() {
        rabbit.stop();
    }

    @DynamicPropertySource
    @SuppressWarnings("unused")
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.addresses", rabbit::getAmqpUrl);

        registry.add("jaconi.rabbitmq.listener.retry.queues.%s.max-attempts".formatted(QUEUE), () -> 2);
        registry.add("jaconi.rabbitmq.listener.retry.queues.%s.durations[0]".formatted(QUEUE), () -> "5s");
        registry.add("jaconi.rabbitmq.listener.retry.queues.%s.durations[1]".formatted(QUEUE), () -> "10s");
    }

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

    @SpringBootApplication
    @SuppressWarnings("unused")
    public static class TestApplication {

        @Bean(QUEUE)
        @SuppressWarnings("unused")
        Queue queue() {
            return new Queue(QUEUE);
        }

        @Bean(EXCHANGE)
        @SuppressWarnings("unused")
        DirectExchange exchange() {
            return new DirectExchange(EXCHANGE);
        }

        @Bean
        @SuppressWarnings("unused")
        Binding binding(@Qualifier(QUEUE) Queue queue, @Qualifier(EXCHANGE) Exchange exchange) {
            return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY).noargs();
        }

        @SuppressWarnings("unused")
        @RabbitListener(queues = QUEUE)
        public void handle(Message message) {
            throw new RetryMessagesException(message);
        }
    }
}
