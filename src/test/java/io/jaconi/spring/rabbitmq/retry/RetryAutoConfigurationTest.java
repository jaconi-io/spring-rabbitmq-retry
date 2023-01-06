package io.jaconi.spring.rabbitmq.retry;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RetryAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class, RetryAutoConfiguration.class));

    @Test
    public void testAutoConfiguration_default_unset() {
        this.contextRunner
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("retryContainerCustomizer");
                });
    }

    @Test
    public void testAutoConfiguration_disabled() {
        this.contextRunner
                .withPropertyValues("jaconi.rabbitmq.listener.retry.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("retryContainerCustomizer");
                });
    }

    @Test
    public void testAutoConfiguration_enabled() {
        this.contextRunner
                .withPropertyValues("jaconi.rabbitmq.listener.retry.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("retryContainerCustomizer");

                    context.getBean(ContainerCustomizer.class);


                });
    }
}
