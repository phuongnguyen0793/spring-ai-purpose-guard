package com.example.springai.domain

// A trained relation between an (industry, business type) and a company name.
// Produced offline by the LLM trainer, stored in a CSV, then loaded into MySQL.
data class CompanyNameRule(
    val id: Long,
    val industryId: String,
    val businessType: String,
    val companyName: String,
    val relation: Relation,
    val dataVersion: Int
)

enum class Relation {
    // The company name is consistent with the industry + business type.
    CONSISTENT,

    // The company name conflicts with / is inappropriate for the industry + business type.
    INCONSISTENT,

    // No strong relation either way.
    NEUTRAL;

    companion object {
        fun fromString(value: String): Relation =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: NEUTRAL
    }
}
