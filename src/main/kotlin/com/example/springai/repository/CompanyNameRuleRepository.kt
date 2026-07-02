package com.example.springai.repository

import com.example.springai.domain.CompanyNameRule
import com.example.springai.domain.Relation
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class CompanyNameRuleRepository(private val jdbc: JdbcTemplate) {

    private val mapper = RowMapper { rs, _ ->
        CompanyNameRule(
            id = rs.getLong("id"),
            industryId = rs.getString("industry_id"),
            businessType = rs.getString("business_type"),
            companyName = rs.getString("company_name"),
            relation = Relation.fromString(rs.getString("relation")),
            dataVersion = rs.getInt("data_version")
        )
    }

    fun findByIndustryAndBusinessType(industryId: String, businessType: String): List<CompanyNameRule> =
        jdbc.query(
            """
            SELECT id, industry_id, business_type, company_name, relation, data_version
            FROM company_name_rule
            WHERE industry_id = ? AND business_type = ?
            """.trimIndent(),
            mapper,
            industryId,
            businessType
        )

    fun insertBatch(rules: List<CompanyNameRule>) {
        if (rules.isEmpty()) return
        jdbc.batchUpdate(
            """
            INSERT INTO company_name_rule
                (industry_id, business_type, company_name, relation, data_version)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            rules.map { rule ->
                arrayOf<Any>(
                    rule.industryId,
                    rule.businessType,
                    rule.companyName,
                    rule.relation.name,
                    rule.dataVersion
                )
            }
        )
    }
}
