package com.example.springai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.training")
data class TrainingProperties(
    // When true, the offline LLM trainer runs on startup, produces the CSV, and exits.
    val enabled: Boolean = false,
    val seedCsvPath: String = "data/company_name_seed.csv",
    val outputCsvPath: String = "data/company_name_rules.csv"
)
