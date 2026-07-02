plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dep.mgmt)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

group = "com.example"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.spring.io/milestone")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

dependencies {
    implementation(libs.spring.boot.web)
    implementation(libs.spring.boot.jdbc)
    implementation(libs.spring.ai.ollama)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    runtimeOnly(libs.mysql.connector)

    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.spring.ai.testcontainers)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.ollama)
    testImplementation(libs.testcontainers.mysql)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

// Force all org.testcontainers artifacts to the catalog version, overriding
// the older version pinned by the Spring Boot 3.4.0 BOM. Required for
// compatibility with Docker Desktop 29.x / Engine API 1.54.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.testcontainers") {
            useVersion(libs.versions.testcontainers.get())
            because("Docker Desktop 29.x requires Testcontainers >= 1.21.3")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Pin the Docker Engine API version requested by docker-java/Testcontainers.
    // Recent docker-java negotiates up to API 1.55, but Docker Desktop 29.x maxes
    // out at 1.54 and rejects 1.55 with HTTP 400. 1.47 is safe across daemons.
    systemProperty("api.version", "1.47")
}
