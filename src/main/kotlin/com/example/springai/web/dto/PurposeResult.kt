package com.example.springai.web.dto

// score is cosine similarity in [0, 1]; higher means more similar.
data class PurposeResult(val id: Long, val purposeText: String, val score: Double)
