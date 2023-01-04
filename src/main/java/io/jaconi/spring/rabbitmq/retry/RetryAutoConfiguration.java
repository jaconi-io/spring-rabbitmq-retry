package io.jaconi.spring.rabbitmq.retry;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "retry.enabled", havingValue = "true")
@EnableConfigurationProperties(RetryProperties.class)
@SuppressWarnings("unused")
public class RetryAutoConfiguration {
    private static final String NOOP_LOGGER = "io.jaconi.spring.rabbitmq.retry.noop";

    @Autowired
    private RetryErrorHandler errorHandler;

    @Bean
    public ContainerCustomizer<SimpleMessageListenerContainer> retryContainerCustomizer() {
        return container -> {
            // Use a custom error handler to take care of RetryMessageExceptions.
            container.setErrorHandler(errorHandler);

            // Configure a noop logger. The only messages written to this logger are stack traces for
            // ImmediateAcknowledgeAmqpExceptions thrown due to RetryMessagesExceptions. These usually should not be
            // logged.
            var logger = (Logger) LoggerFactory.getLogger(NOOP_LOGGER);
            logger.setLevel(Level.OFF);
            container.setErrorHandlerLoggerName(NOOP_LOGGER);
        };
    }
}
