package com.example.springai.startup

import com.example.springai.config.SearchProperties
import com.example.springai.repository.BusinessPurposeRepository
import com.example.springai.vectorstore.InMemoryVectorStore
import org.slf4j.LoggerFactory
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

// On startup: seed sample purposes (if empty), ensure every purpose has a
// persisted embedding, then load them all into the in-memory vector store.
@Component
@Order(20)
class PurposeIndexInitializer(
    private val purposeRepository: BusinessPurposeRepository,
    private val embeddingModel: EmbeddingModel,
    private val vectorStore: InMemoryVectorStore,
    private val searchProps: SearchProperties
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val samplePurposes = listOf(
        "Provide cloud-native payments infrastructure",
        "Develop AI-powered healthcare diagnostics",
        "Offer e-commerce platform for small retailers",
        "Create mobile games for casual players",
        "Consulting for digital transformation"
    )

    override fun run(vararg args: String?) {
        if (purposeRepository.count() == 0 && searchProps.seedOnStartup) {
            logger.info("business_purpose is empty — seeding {} sample purposes", samplePurposes.size)
            samplePurposes.forEach { purposeRepository.insert(it, null) }
        }

        val purposes = purposeRepository.findAll()
        var computed = 0
        val entries = purposes.map { purpose ->
            val embedding = purpose.embedding ?: run {
                val fresh = embeddingModel.embed(purpose.purposeText)
                purposeRepository.updateEmbedding(purpose.id, fresh)
                computed++
                fresh
            }
            InMemoryVectorStore.Entry(purpose.id, purpose.purposeText, embedding)
        }

        vectorStore.load(entries)
        logger.info(
            "Loaded {} purposes into in-memory vector store ({} embeddings computed this run)",
            vectorStore.size(),
            computed
        )
    }
}
