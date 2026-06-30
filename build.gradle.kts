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
    implementation(libs.spring.ai.ollama)
    implementation(libs.spring.ai.opensearch)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)

    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.spring.ai.testcontainers)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.ollama)
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

tasks.withType<Test> { useJUnitPlatform() }
