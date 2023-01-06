package io.jaconi.spring.rabbitmq.retry;

import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
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

@Configuration
@ConditionalOnProperty(value = {"jaconi.rabbitmq.listener.retry.enabled",
        "jaconi.rabbitmq.listener.retry.create-resources"}, havingValue = "true")
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
        for (var sourceQueue : properties.queues().keySet()) {
            // Create a dedicated dead letter exchange per source queue. The dead letter exchange is used, when
            // all retries failed.
            var dlxName = RetryProperties.DEAD_LETTER_EXCHANGE_PATTERN.formatted(sourceQueue);
            var dlx = ExchangeBuilder.topicExchange(dlxName).build();
            beanFactory.registerSingleton(dlx.toString(), dlx);

            // Create a dedicated retry exchange per source queue.
            var retryExchangeName = RetryProperties.RETRY_EXCHANGE_PATTERN.formatted(sourceQueue);
            var retryExchange = ExchangeBuilder.headersExchange(retryExchangeName).alternate(dlxName).build();
            beanFactory.registerSingleton(retryExchange.toString(), retryExchange);

            // Create a dedicated dead letter queue per source queue.
            var dlqName = RetryProperties.DEAD_LETTER_QUEUE_PATTERN.formatted(sourceQueue);
            var dlq = QueueBuilder.durable(dlqName).build();
            beanFactory.registerSingleton(dlq.toString(), dlq);

            // Bind the dead letter queue to the dead letter exchange.
            var dlqBinding = BindingBuilder.bind(dlq).to(dlx).with("#").noargs();
            beanFactory.registerSingleton(dlqBinding.toString(), dlqBinding);

            // Create a dedicated dispatch exchange per source queue (to redirect back to the source queue).
            var dispatchExchangeName = RetryProperties.DISPATCH_EXCHANGE_PATTERN.formatted(sourceQueue);
            var dispatchExchange = ExchangeBuilder.topicExchange(dispatchExchangeName).build();
            beanFactory.registerSingleton(dispatchExchange.toString(), dispatchExchange);

            // Bind the original source queue to the dispatch exchange.
            var dispatchBinding = new Binding(sourceQueue, Binding.DestinationType.QUEUE, dispatchExchangeName, "#",
                    Collections.emptyMap());
            beanFactory.registerSingleton(dispatchBinding.toString(), dispatchBinding);

            // Define the required retry queues.
            var queueProperties = properties.queues().get(sourceQueue);
            var queues = queueProperties.durations().stream()
                    .map(duration -> {
                        var queueName = RetryProperties.RETRY_QUEUE_PATTERN.formatted(sourceQueue, duration);
                        return QueueBuilder.durable(queueName)
                                .ttl((int) duration.toMillis())
                                .deadLetterExchange(dispatchExchangeName)
                                .build();
                    })
                    .toList();

            // Create the required retry queues.
            queues.forEach(queue -> beanFactory.registerSingleton(queue.toString(), queue));

            // Bind the retry queues to the retry exchange. Bind any additional attempts to the last queue.
            int bindings = queueProperties.durations().size();
            if (queueProperties.maxRetries() != null) {
                bindings = Math.max(queueProperties.maxRetries(), queueProperties.durations().size());
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
