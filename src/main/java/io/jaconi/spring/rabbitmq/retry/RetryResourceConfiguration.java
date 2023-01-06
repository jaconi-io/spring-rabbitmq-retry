package io.jaconi.spring.rabbitmq.retry;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

import java.util.Map;

@Configuration
@ConditionalOnProperty({"retry.enabled", "retry.create-resources"})
@RequiredArgsConstructor
public class RetryResourceConfiguration implements BeanFactoryAware, InitializingBean {
    private final RetryProperties properties;

    private ConfigurableBeanFactory beanFactory;

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableBeanFactory) beanFactory;
    }

    @Override
    public void afterPropertiesSet() {
        for (var targetExchange : properties.exchanges().keySet()) {
            // Create a dedicated dead letter exchange per target exchange. The dead letter exchange is used, when
            // all retries failed.
            var dlxName = RetryProperties.DEAD_LETTER_EXCHANGE_PATTERN.formatted(targetExchange);
            var dlx = ExchangeBuilder.topicExchange(dlxName).build();
            beanFactory.registerSingleton(dlx.toString(), dlx);

            // Create a dedicated retry exchange per target exchange.
            var retryExchangeName = RetryProperties.RETRY_EXCHANGE_PATTERN.formatted(targetExchange);
            var retryExchange = ExchangeBuilder.headersExchange(retryExchangeName).alternate(dlxName).build();
            beanFactory.registerSingleton(retryExchange.toString(), retryExchange);

            // Create a dedicated dead letter queue per target exchange.
            var dlqName = RetryProperties.DEAD_LETTER_QUEUE_PATTERN.formatted(targetExchange);
            var dlq = QueueBuilder.durable(dlqName).build();
            beanFactory.registerSingleton(dlq.toString(), dlq);

            // Bind the dead letter queue to the dead letter exchange.
            var dlqBinding = BindingBuilder.bind(dlq).to(dlx).with("#").noargs();
            beanFactory.registerSingleton(dlqBinding.toString(), dlqBinding);

            // Define the required retry queues.
            var exchangeProperties = properties.exchanges().get(targetExchange);
            var queues = exchangeProperties.durations().stream()
                    .map(duration -> {
                        var queueName = RetryProperties.RETRY_QUEUE_PATTERN.formatted(targetExchange, duration);
                        return QueueBuilder.durable(queueName)
                                .ttl((int) duration.toMillis())
                                .deadLetterExchange(targetExchange)
                                .build();
                    })
                    .toList();

            // Create the required retry queues.
            queues.forEach(queue -> beanFactory.registerSingleton(queue.toString(), queue));

            // Bind the retry queues to the retry exchange. Bind any additional attempts to the last queue.
            int bindings = exchangeProperties.durations().size();
            if (exchangeProperties.maxRetries() != null) {
                bindings = Math.max(exchangeProperties.maxRetries(), exchangeProperties.durations().size());
            }
            for (int i = 0; i < bindings; i++) {
                var queue = queues.get(Math.min(i, queues.size() - 1));
                var binding = BindingBuilder.bind(queue).to(retryExchange).with("").and(Map.of(
                        RetryProperties.RETRY_HEADER, i + 1,
                        "x-match", "any-with-x"
                ));
                beanFactory.registerSingleton(binding.toString(), binding);
            }
        }
    }
}
