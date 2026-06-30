package com.example.springai

import org.testcontainers.DockerClientFactory
import org.springframework.boot.fromApplication
import org.springframework.boot.with

/**
 * Local dev entry point — starts the app with Testcontainers instead of
 * a running Docker Compose stack. Run this class directly from the IDE or:
 *
 *   ./gradlew bootTestRun
 *
 * Requires Docker Desktop to be running.
 * On first run, Testcontainers pulls the images and the Ollama model (~1-2 GB).
 * Subsequent runs reuse the committed Docker image and start in seconds.
 */
fun main(args: Array<String>) {
    check(DockerClientFactory.instance().isDockerAvailable) {
        """
        Docker is not running. Please start Docker Desktop and try again.
          macOS: open -a Docker
        """.trimIndent()
    }

    fromApplication<SpringAiApplication>()
        .with(TestcontainersConfiguration::class)
        .run(*args)
}
