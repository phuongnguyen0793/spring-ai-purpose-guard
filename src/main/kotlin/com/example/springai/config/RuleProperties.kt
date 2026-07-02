package com.example.springai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.rule")
data class RuleProperties(
    // Filesystem path of the trained CSV loaded into MySQL on startup.
    val csvPath: String = "data/company_name_rules.csv"
)
