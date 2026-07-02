package com.example.springai.repository

import com.example.springai.domain.BusinessPurpose
import com.example.springai.util.EmbeddingJson
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.Statement

@Repository
class BusinessPurposeRepository(private val jdbc: JdbcTemplate) {

    private val mapper = RowMapper { rs, _ ->
        BusinessPurpose(
            id = rs.getLong("id"),
            purposeText = rs.getString("purpose_text"),
            embedding = EmbeddingJson.fromJson(rs.getString("embedding"))
        )
    }

    fun findAll(): List<BusinessPurpose> =
        jdbc.query("SELECT id, purpose_text, embedding FROM business_purpose ORDER BY id", mapper)

    fun count(): Int =
        jdbc.queryForObject("SELECT COUNT(*) FROM business_purpose", Int::class.java) ?: 0

    fun insert(purposeText: String, embedding: FloatArray?): Long {
        val keyHolder = GeneratedKeyHolder()
        jdbc.update({ con ->
            val ps = con.prepareStatement(
                "INSERT INTO business_purpose (purpose_text, embedding) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
            ps.setString(1, purposeText)
            ps.setString(2, embedding?.let { EmbeddingJson.toJson(it) })
            ps
        }, keyHolder)
        return keyHolder.key?.toLong() ?: error("Failed to obtain generated id for business_purpose")
    }

    fun updateEmbedding(id: Long, embedding: FloatArray) {
        jdbc.update(
            "UPDATE business_purpose SET embedding = ? WHERE id = ?",
            EmbeddingJson.toJson(embedding),
            id
        )
    }
}
