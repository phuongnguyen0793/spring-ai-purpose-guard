package com.example.springai

import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.images.ImagePullPolicy
import org.testcontainers.ollama.OllamaContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val OLLAMA_MODEL = "nomic-embed-text"
        private const val OLLAMA_CACHED_IMAGE = "tc-ollama-$OLLAMA_MODEL:latest"
    }

    // MySQL container — @ServiceConnection auto-wires spring.datasource.* and
    // Spring Boot runs schema.sql against it on startup.
    @Bean
    @ServiceConnection
    fun mysqlContainer(): MySQLContainer<*> =
        MySQLContainer(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("sample_ai")
            .withUsername("dev")
            .withPassword("dev")

    // Ollama container with the embedding model pre-pulled and committed to a
    // reusable image so subsequent runs start quickly.
    // @ServiceConnection auto-wires spring.ai.ollama.base-url.
    @Bean
    @ServiceConnection
    fun ollamaContainer(): OllamaContainer {
        return if (imageExists(OLLAMA_CACHED_IMAGE)) {
            logger.info("Reusing cached Ollama image '$OLLAMA_CACHED_IMAGE'")
            OllamaContainer(
                DockerImageName.parse(OLLAMA_CACHED_IMAGE)
                    .asCompatibleSubstituteFor("ollama/ollama")
            )
                // The cached image is local-only (created via commitToImage), so it
                // must never be resolved against a registry — that would 404.
                .withImagePullPolicy(ImagePullPolicy { false })
        } else {
            logger.info("Pulling Ollama model '$OLLAMA_MODEL' (first run — this may take a few minutes)…")
            val container = OllamaContainer("ollama/ollama:latest")
            container.start()
            container.execInContainer("ollama", "pull", OLLAMA_MODEL)
            container.commitToImage(OLLAMA_CACHED_IMAGE)
            logger.info("Model committed to image '$OLLAMA_CACHED_IMAGE' — future runs will be instant")
            container
        }
    }

    // Reliable local-image existence check: compares exact RepoTags rather than
    // relying on the server-side reference filter (which can be inconsistent on
    // Docker Desktop and yield false positives).
    private fun imageExists(image: String): Boolean =
        runCatching {
            DockerClientFactory.lazyClient()
                .listImagesCmd()
                .exec()
                .any { img -> img.repoTags?.any { it == image } == true }
        }.getOrDefault(false)
}
