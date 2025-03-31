package io.jaconi.spring.rabbitmq.retry;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;

@SpringBootTest(properties = {
        "jaconi.rabbitmq.listener.retry.enabled=true",
        "jaconi.rabbitmq.listener.retry.create-resources=true"
})
abstract class RabbitMQTest {
    protected static final String EXCHANGE = "test-exchange";
    protected static final String QUEUE = "test-queue";
    protected static final String ROUTING_KEY = "foo";

    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.0-management-alpine");

    @Autowired
    @SuppressWarnings("unused")
    protected AmqpTemplate amqpTemplate;

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

    @SpringBootApplication
    protected abstract static class RabbitMQTestApplication {

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
    }
}
