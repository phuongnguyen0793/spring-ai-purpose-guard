package com.example.springai.web.dto

data class ValidateNameResponse(
    val companyName: String,
    val industryId: String,
    val businessType: String,
    val valid: Boolean,
    val reasons: List<String>,
    val relation: String,
    val matchedExample: String?,
    val matchScore: Double,
    val rulesEvaluated: Int,
    val dataVersion: Int?
)
