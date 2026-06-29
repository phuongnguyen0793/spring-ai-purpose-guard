package com.example.springai.config

import com.example.springai.integration.OllamaClient
import org.springframework.boot.CommandLineRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class StartupEmbeddingInitializer(private val jdbc: JdbcTemplate, private val ollama: OllamaClient) : CommandLineRunner {
    override fun run(vararg args: String?) {
        val rows = jdbc.queryForList("SELECT id, purpose_text FROM purposes WHERE embedding IS NULL")
        for (r in rows) {
            val id = r["id"] as Number
            val text = r["purpose_text"] as String
            try {
                val emb = ollama.embed(text)
                val vec = emb.joinToString(",")
                val sql = "UPDATE purposes SET embedding = ('[" + vec + "]')::vector WHERE id = ?"
                jdbc.update(sql, id.toLong())
            } catch (e: Exception) {
                println("Failed to compute embedding for id=${'$'}id: ${'$'}{e.message}")
            }
        }
    }
}
