package com.example.springai.web.dto

data class PurposeRequest(
    val purpose: String,
    val topK: Int? = null,        // Defaults to app.search.default-top-k
    val threshold: Double? = null // Defaults to app.search.default-threshold
)
