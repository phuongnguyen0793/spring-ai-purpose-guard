package com.example.springai.service

import com.example.springai.config.SearchProperties
import com.example.springai.repository.BusinessPurposeRepository
import com.example.springai.vectorstore.InMemoryVectorStore
import com.example.springai.web.dto.PurposeResult
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Service

// Semantic search over business purposes. Embeddings live in MySQL and are
// loaded into the in-memory index at startup; queries are embedded on demand
// via Spring AI's EmbeddingModel and compared in memory.
@Service
class PurposeSearchService(
    private val embeddingModel: EmbeddingModel,
    private val vectorStore: InMemoryVectorStore,
    private val purposeRepository: BusinessPurposeRepository,
    private val searchProps: SearchProperties
) {

    fun search(
        purposeText: String,
        topK: Int? = null,
        threshold: Double? = null
    ): List<PurposeResult> {
        if (vectorStore.size() == 0) return emptyList()
        val queryEmbedding = embeddingModel.embed(purposeText)
        return vectorStore
            .search(
                queryEmbedding = queryEmbedding,
                topK = topK ?: searchProps.defaultTopK,
                threshold = threshold ?: searchProps.defaultThreshold
            )
            .map { PurposeResult(id = it.id, purposeText = it.text, score = it.score) }
    }

    // Persist a new purpose (with its embedding) to MySQL and add it to the
    // in-memory index so it is immediately searchable.
    fun addPurpose(purposeText: String): PurposeResult {
        val embedding = embeddingModel.embed(purposeText)
        val id = purposeRepository.insert(purposeText, embedding)
        vectorStore.add(InMemoryVectorStore.Entry(id, purposeText, embedding))
        return PurposeResult(id = id, purposeText = purposeText, score = 1.0)
    }
}
