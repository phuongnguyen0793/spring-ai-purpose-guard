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
    // Pin the Docker Engine API version the docker-java client (used by
    // Testcontainers) requests. Recent docker-java negotiates up to API 1.55,
    // but Docker Desktop 29.x maxes out at 1.54 and rejects 1.55 with HTTP 400
    // ("Could not find a valid Docker environment"). 1.47 is within every
    // recent daemon's supported range (1.40–1.54+), so this is safe cross-env.
    System.setProperty("api.version", "1.47")

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
