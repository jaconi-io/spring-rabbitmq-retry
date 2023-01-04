# Spring RabbitMQ Retry

Retries and exponential backoff for Spring AMQP.

## Publishing

Spring RabbitMQ Retry is published to the central maven repository.

Usually, publishing happens automatically via GitHub Actions. However, if you are an employee of jaconi, you can also
publish releases manually. To publish a release, you will need to configure the GPG private signing key and the keys
passphrase:

```
export ORG_GRADLE_PROJECT_signingKey=<GPG signing key>
export ORG_GRADLE_PROJECT_signingPassword=<GPG signing key passphrase>
```

If you are having issues setting the multiline signing key as an environment variable, you can use Base64 encoding.

Additionally, you will need credentials for [https://s01.oss.sonatype.org](https://s01.oss.sonatype.org). Configure
these in `~/.gradle/gradle.properties` like this:

```
ossrhUsername=<username>
ossrhPassword=<password>
```

Once everything is set up, you should be able to publish snapshots using

```
./gradlew publish
```

If your [https://s01.oss.sonatype.org](https://s01.oss.sonatype.org) credentials do not have sufficient privileges,
create a ticket for manual approval, as described here:
[https://central.sonatype.org/publish/manage-permissions/](https://central.sonatype.org/publish/manage-permissions/)
