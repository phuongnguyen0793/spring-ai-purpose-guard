package com.example.springai.vectorstore

import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sqrt

// A lightweight in-memory semantic index. Embeddings are persisted in MySQL and
// loaded here on startup; searches run entirely in memory via cosine similarity.
@Component
class InMemoryVectorStore {

    data class Entry(val id: Long, val text: String, val embedding: FloatArray)

    data class ScoredEntry(val id: Long, val text: String, val score: Double)

    private val entries = CopyOnWriteArrayList<Entry>()

    fun load(items: List<Entry>) {
        entries.clear()
        entries.addAll(items)
    }

    fun add(entry: Entry) {
        entries.add(entry)
    }

    fun size(): Int = entries.size

    fun search(queryEmbedding: FloatArray, topK: Int, threshold: Double): List<ScoredEntry> =
        entries.asSequence()
            .map { ScoredEntry(it.id, it.text, cosineSimilarity(queryEmbedding, it.embedding)) }
            .filter { it.score >= threshold }
            .sortedByDescending { it.score }
            .take(topK)
            .toList()

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0.0
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0.0) 0.0 else dot / denom
    }
}
