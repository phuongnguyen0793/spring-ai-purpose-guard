package com.example.springai

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.ollama.OllamaContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val OLLAMA_MODEL = "nomic-embed-text"
        private const val OLLAMA_CACHED_IMAGE = "tc-ollama-$OLLAMA_MODEL:latest"
    }

    /**
     * Ollama container with the embedding model pre-pulled.
     *
     * First run: starts the base image, pulls the model (~600 MB), then commits
     * a new local Docker image so the model survives container restarts.
     * Subsequent runs: reuses the committed image and start in seconds.
     *
     * @ServiceConnection auto-wires spring.ai.ollama.base-url to the mapped port.
     */
    @Bean
    @ServiceConnection
    fun ollamaContainer(): OllamaContainer {
        val cachedImageExists = DockerClientFactory.lazyClient()
            .listImagesCmd()
            .withImageNameFilter(OLLAMA_CACHED_IMAGE)
            .exec()
            .isNotEmpty()

        return if (cachedImageExists) {
            logger.info("Reusing cached Ollama image '$OLLAMA_CACHED_IMAGE'")
            OllamaContainer(
                DockerImageName.parse(OLLAMA_CACHED_IMAGE)
                    .asCompatibleSubstituteFor("ollama/ollama")
            )
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

    /**
     * OpenSearch container — no built-in @ServiceConnection for Spring AI's vector store,
     * so the URI is registered via DynamicPropertyRegistrar.
     */
    @Bean
    fun openSearchContainer(): GenericContainer<*> =
        GenericContainer("opensearchproject/opensearch:2.13.0")
            .withExposedPorts(9200)
            .withEnv("discovery.type", "single-node")
            .withEnv("plugins.security.disabled", "true")
            .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
            .waitingFor(
                Wait.forHttp("/_cluster/health")
                    .forPort(9200)
                    .forStatusCode(200)
            )

    @Bean
    fun openSearchProperties(@Qualifier("openSearchContainer") openSearchContainer: GenericContainer<*>): DynamicPropertyRegistrar =
        DynamicPropertyRegistrar { registry ->
            registry.add("spring.ai.vectorstore.opensearch.uris") {
                "http://${openSearchContainer.host}:${openSearchContainer.getMappedPort(9200)}"
            }
        }
}
