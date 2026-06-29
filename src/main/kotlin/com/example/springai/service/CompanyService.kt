package com.example.springai.service

import com.example.springai.util.JapaneseCompanyNameValidator
import com.example.springai.web.dto.PurposeResult
import com.example.springai.integration.OllamaClient
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service

@Service
class CompanyService(
    private val nameValidator: JapaneseCompanyNameValidator,
    private val ollamaClient: OllamaClient,
    private val jdbc: JdbcTemplate
) {

    fun validateCompanyName(name: String): Map<String, Any> {
        val valid = nameValidator.isValid(name)
        val reasons = if (!valid) nameValidator.validateReasons(name) else emptyList<String>()
        return mapOf("companyName" to name, "valid" to valid, "reasons" to reasons)
    }

    fun searchSimilarPurposes(purposeText: String, limit: Int): List<PurposeResult> {
        val embedding = ollamaClient.embed(purposeText)
        // convert float array to comma separated string
        val vecStr = embedding.joinToString(",")
        // pgvector similarity: '<->' operator expects a vector literal syntax like '[x,y,...]'
        val sql = "SELECT id, purpose_text, (embedding <-> ('[" + vecStr + "]')::vector) as distance FROM purposes ORDER BY distance ASC LIMIT ?"
        val mapper = RowMapper { rs, _ -> PurposeResult(rs.getLong("id"), rs.getString("purpose_text"), rs.getDouble("distance")) }
        val rows = jdbc.query(sql, arrayOf(limit), mapper)
        return rows
    }
}
