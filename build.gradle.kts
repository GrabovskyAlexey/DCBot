plugins {
    id("jacoco")
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.2.0"
}
group = "ru.grabovsky"
version = "0.0.1-SNAPSHOT"
val telegramBotVersion = "9.0.0"
val testcontainersVersion = "1.20.1"
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}
repositories {
    mavenCentral()
}
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-freemarker")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.3.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.liquibase:liquibase-core")
    implementation("io.github.oshai:kotlin-logging:7.0.0")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("org.telegram:telegrambots-springboot-longpolling-starter:${telegramBotVersion}")
    implementation("org.telegram:telegrambots-extensions:${telegramBotVersion}")
    implementation("org.telegram:telegrambots-client:${telegramBotVersion}")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.testcontainers:testcontainers:${testcontainersVersion}")
    testImplementation("org.testcontainers:postgresql:${testcontainersVersion}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
tasks.withType<Test> {
    useJUnitPlatform()
    filter {
        excludeTestsMatching("ru.grabovsky.dungeoncrusherbot.DungeoncrusherbotApplicationTests")
    }
}
jacoco {
    toolVersion = "0.8.11"
}
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude("ru/grabovsky/dungeoncrusherbot/config/**")
            exclude("ru/grabovsky/dungeoncrusherbot/dto/**")
            exclude("ru/grabovsky/dungeoncrusherbot/entity/**")
        }
    }))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}


