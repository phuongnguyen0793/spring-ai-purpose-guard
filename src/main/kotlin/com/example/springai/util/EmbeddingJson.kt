package com.example.springai.util

import com.fasterxml.jackson.databind.ObjectMapper

// Serializes embeddings (float arrays) to/from a compact JSON array string
// for storage in a MySQL LONGTEXT column.
object EmbeddingJson {

    private val mapper = ObjectMapper()

    fun toJson(embedding: FloatArray): String = mapper.writeValueAsString(embedding)

    fun fromJson(json: String?): FloatArray? {
        if (json.isNullOrBlank()) return null
        return mapper.readValue(json, FloatArray::class.java)
    }
}
