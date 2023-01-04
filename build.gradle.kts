plugins {
	java
	id("io.spring.dependency-management") version "1.1.0"
}

group = "io.jaconi"
version = "0.0.1-SNAPSHOT"

dependencyManagement {
	imports {
		mavenBom("org.springframework.boot:spring-boot-dependencies:3.0.1")
	}
}

dependencies {
	annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	annotationProcessor("org.projectlombok:lombok")
	compileOnly("org.projectlombok:lombok")

	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.amqp:spring-rabbit")

	testImplementation("org.junit.jupiter:junit-jupiter-api")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}

repositories {
	mavenCentral()
}

tasks.withType<Test> {
	useJUnitPlatform()
}
