import java.util.Base64

plugins {
	`java-library`
	`maven-publish`
	signing
	id("io.spring.dependency-management") version "1.1.3"
}

group = "io.jaconi"
version = "1.1.6"

if (project.properties["release"] != "true") {
	project.version = "${project.version}-SNAPSHOT"
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.boot:spring-boot-dependencies:3.1.4")
	}
}

dependencies {
	annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	annotationProcessor("org.projectlombok:lombok")
	compileOnly("org.projectlombok:lombok")

	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.amqp:spring-rabbit")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.junit.jupiter:junit-jupiter-api")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}

	withJavadocJar()
	withSourcesJar()
}

repositories {
	mavenCentral()
}

tasks.withType<Test> {
	useJUnitPlatform()
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			from(components["java"])

			versionMapping {
				usage("java-api") {
					fromResolutionOf("runtimeClasspath")
				}
				usage("java-runtime") {
					fromResolutionResult()
				}
			}

			repositories {
				maven {
					credentials {
						// Configure by setting the ORG_GRADLE_PROJECT_ossrhUsername environment variable.
						val ossrhUsername: String? by project
						username = ossrhUsername

						// Configure by setting the ORG_GRADLE_PROJECT_ossrhPassword environment variable.
						val ossrhPassword: String? by project
						password = ossrhPassword
					}

					url = if (version.endsWith("-SNAPSHOT")) {
						uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
					} else {
						uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
					}
				}
			}

			pom {
				name.set("Spring RabbitMQ Retry")
				description.set("Retries and exponential backoff for Spring AMQP.")
				url.set("https://github.com/jaconi-io/spring-rabbitmq-retry")
				inceptionYear.set("2023")

				licenses {
					license {
						name.set("MIT")
						url.set("https://opensource.org/licenses/MIT")
						distribution.set("repo")
						comments.set("MIT License")
					}
				}

				organization {
					name.set("jaconi GmbH")
					url.set("https://jaconi.io")
				}

				developers {
					developer {
						name.set("Julian Nodorp")
						email.set("jnodorp@jaconi.io")
						organization.set("jaconi GmbH")
						organizationUrl.set("https://jaconi.io")
					}

					developer {
						name.set("Gerrit Schlüter")
						email.set("gschlueter@jaconi.io")
						organization.set("jaconi GmbH")
						organizationUrl.set("https://jaconi.io")
					}
				}

				issueManagement {
					system.set("GitHub Issues")
					url.set("https://github.com/jaconi-io/spring-rabbitmq-retry/issues")
				}

				ciManagement {
					system.set("GitHub Actions")
					url.set("https://github.com/jaconi-io/spring-rabbitmq-retry/actions")
				}

				scm {
					connection.set("scm:git:https://github.com/jaconi-io/spring-rabbitmq-retry.git")
					developerConnection.set("scm:git:ssh://github.com:jaconi-io/spring-rabbitmq-retry.git")
					url.set("https://github.com/jaconi-io/spring-rabbitmq-retry/tree/main")
				}
			}
		}
	}
}

signing {
	// Configure by setting the ORG_GRADLE_PROJECT_signingKey environment variable.
	val signingKey: String? by project

	// Configure by setting the ORG_GRADLE_PROJECT_signingPassword environment variable.
	val signingPassword: String? by project

	useInMemoryPgpKeys(base64DecodeIfEncoded(signingKey), signingPassword)

	isRequired = gradle.taskGraph.hasTask("publish")
	sign(publishing.publications["maven"])
}

/**
 * Base64 decode an input. If the input is not Base64 encoded, return the input.
 */
fun base64DecodeIfEncoded(s: String?): String? {
	if (s == null) {
		return null
	}

	return try {
		String(Base64.getDecoder().decode(s))
	} catch (_: IllegalArgumentException) {
		s
	}
}
