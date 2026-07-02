package com.example.springai.domain

// A business purpose plus its (optional) precomputed embedding.
// The embedding is stored as a float array; persisted to MySQL as a JSON string.
data class BusinessPurpose(
    val id: Long,
    val purposeText: String,
    val embedding: FloatArray?
)
