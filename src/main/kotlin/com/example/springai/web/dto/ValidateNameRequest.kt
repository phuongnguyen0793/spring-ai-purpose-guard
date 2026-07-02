package com.example.springai.web.dto

data class ValidateNameRequest(
    val companyName: String,
    val industryId: String,
    val businessType: String
)
