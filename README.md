[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/jaconi-io/spring-rabbitmq-retry/continuous.yaml?branch=main)](https://github.com/jaconi-io/spring-rabbitmq-retry/actions/workflows/continuous.yaml)
[![GitHub License](https://img.shields.io/github/license/jaconi-io/spring-rabbitmq-retry)](https://github.com/jaconi-io/spring-rabbitmq-retry?tab=MIT-1-ov-file#readme)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.jaconi/spring-rabbitmq-retry)](https://central.sonatype.com/artifact/io.jaconi/spring-rabbitmq-retry)

# Spring RabbitMQ Retry

Retries and exponential backoff for Spring AMQP.

## Motivation

When using Spring RabbitMQ, a listener might look like this:

```java
class MyListener {
    @RabbitListener(queues = "foo")
    public void handle(Message msg) {
        processMessage(msg);
    }
}
```

By default, any exception thrown in the listener will immediately lead to the message being re-queued. In many cases
exceptions are caused by either malformed messages or unavailable backends. In both cases, re-queuing the message will
not help. If a backend is unavailable due to being overloaded, this behavior is harmful.

A solution to this problem is exponential backoff. A message will not be retried immediately, but after some delay. For
each failure, the delay is increased exponentially, until a maximum number of retries is reached. Then, the message is
sent to a dead letter queue for manual intervention.

## Message Flow

![Message Flow](docs/flow.svg)

A message is sent to the `in` exchange via AMQP. It is then routed to a number of queues. One of them is the queue
`foo`, that we will use for this example. There is a Spring application with a listener on `foo`. Some minimal listener
code might look like this:

```java
class MyListener {
    @RabbitListener(queues = "foo")
    public void handle(Message msg) {
        try {
            processMessage(msg);
        } catch (BackendTimeoutException e) {
            // Backend will probably come back. Retry.
            throw new RetryMessagesException(msg);
        } catch (MalformedMessageException e) {
            // The message will not be fixed by retrying...
            // Log and discard.
        }
    }
}
```

This library will handle `RetryMessagesExceptions` thrown by listeners, using the flow depicted above:

The message is sent to a retry exchange, dedicated to the original queue (`foo-retry`) with a header indicating the
number of previous attempts plus one. The retry exchange routes the message based on the retry header. 

If the retry attempts are exhausted, and the message cannot be routed, the alternate exchange is used, which just routes
the message to a dead letter queue (`foo-retry-never`) for manual intervention.

The retry queues have a timeout configured, that will dead letter the message. However, the dead letter exchange is
`foo-dispatch`, that will just route the message back to the original queue.

## Configuration

The library is configurable via the typical Spring properties. Find below an annotated example configuration.

```yaml
jaconi:
  rabbitmq:
    listener:
      retry:
        # Enable or disable the retry functionality entirely.
        enabled: true
        # Create the RabbitMQ resources (exchanges and queues) programmatically at startup.
        create-resources: true
        # Configure the source queues. Messages from these queues will be retried.
        queues: 
          # Messages originating from a queue named com.example.exchange will be retried.
          "[com.example.queue]":
              # A message will be retried 5 times before being sent to a DLX or discarded.
              max-attempts: 5
              # Retry attempts after 30s, 2m and 5m
              durations:
                - 30s
                - 2m
                - 5m
```

If you set `create-resources = true` you need to ensure that the RabbitMQ user that your application is using has the 
required permissions to declare (configure) the required queues.

## Manual Acknowledgement

When dealing with situations where the retry error handler cannot be used (for example, when dealing with manual `ack`
and `nack`), the `RetryService` can be used directly:

```java
class MyListener {
    
    @Autowired
    private RetryService retryService;
    
    @RabbitListener(queues = "foo")
    public void handle(Message msg, Channel ch) {
        try {
            processMessage(msg);

            // Acknowledge successfully processed messages.
            ch.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (BackendTimeoutException e) {
            // Backend will probably come back. Retry.
            retryService.retry(msg);

            // Acknowledge messages scheduled for retry.
            ch.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (MalformedMessageException e) {
            // The message will not be fixed by retrying...
            // Log and discard.
            ch.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }
}
```

## Releasing

Spring RabbitMQ Retry is published to the central maven repository.

Usually, publishing happens automatically via GitHub Actions. However, if you are an employee of jaconi, you can also
publish releases manually. To publish a release, you will need to configure the GPG private signing key and the key's
passphrase:

```
export ORG_GRADLE_PROJECT_signingKey=<GPG signing key>
export ORG_GRADLE_PROJECT_signingPassword=<GPG signing key passphrase>
```

If you are having issues setting the multiline signing key as an environment variable, you can use Base64 encoding.

Additionally, you will need credentials for [https://central.sonatype.com](https://central.sonatype.com). Configure
these in `~/.gradle/gradle.properties` like this:

```
sonatypeUsername=<username>
sonatypePassword=<password>
```

Once everything is set up, you should be able to publish snapshots using

```
./gradlew publishAllPublicationsToCentralPortal
```
