package com.example.springai.service

import com.example.springai.domain.CompanyNameRule
import com.example.springai.domain.Relation
import com.example.springai.repository.CompanyNameRuleRepository
import com.example.springai.repository.DataVersionRepository
import com.example.springai.util.JapaneseCompanyNameValidator
import com.example.springai.web.dto.ValidateNameResponse
import org.springframework.stereotype.Service

// Data-driven, rule-based company name validation. Combines baseline format
// checks with the trained (industry_id, business_type, company_name, relation)
// data loaded from CSV into MySQL.
@Service
class CompanyNameValidationService(
    private val formatValidator: JapaneseCompanyNameValidator,
    private val ruleRepository: CompanyNameRuleRepository,
    private val dataVersionRepository: DataVersionRepository
) {

    companion object {
        const val RULE_DATASET = "company_name_rule"
        private const val MATCH_ACCEPT_THRESHOLD = 0.5
        private val NAME_NOISE = listOf("株式会社", "有限会社", "合同会社", "inc", "co", "ltd", "corp", "corporation", "kk")
    }

    fun validate(companyName: String, industryId: String, businessType: String): ValidateNameResponse {
        val reasons = formatValidator.validateReasons(companyName).toMutableList()

        val rules = ruleRepository.findByIndustryAndBusinessType(industryId, businessType)
        val best = bestMatch(companyName, rules)

        val relation = when {
            best == null -> Relation.NEUTRAL
            best.score >= MATCH_ACCEPT_THRESHOLD -> best.rule.relation
            else -> Relation.NEUTRAL
        }

        if (relation == Relation.INCONSISTENT) {
            reasons.add(
                "Name is inconsistent with industry '$industryId' / business type " +
                    "'$businessType' (matched trained example '${best?.rule?.companyName}')"
            )
        }

        return ValidateNameResponse(
            companyName = companyName,
            industryId = industryId,
            businessType = businessType,
            valid = reasons.isEmpty(),
            reasons = reasons,
            relation = relation.name,
            matchedExample = best?.rule?.companyName,
            matchScore = best?.score ?: 0.0,
            rulesEvaluated = rules.size,
            dataVersion = dataVersionRepository.maxVersion(RULE_DATASET)
        )
    }

    private data class Match(val rule: CompanyNameRule, val score: Double)

    private fun bestMatch(companyName: String, rules: List<CompanyNameRule>): Match? {
        val queryTokens = tokenize(companyName)
        if (rules.isEmpty()) return null
        return rules
            .map { Match(it, similarity(queryTokens, tokenize(it.companyName), companyName, it.companyName)) }
            .maxByOrNull { it.score }
    }

    // Exact normalized match scores 1.0; otherwise Jaccard token overlap.
    private fun similarity(
        aTokens: Set<String>,
        bTokens: Set<String>,
        aRaw: String,
        bRaw: String
    ): Double {
        if (normalize(aRaw) == normalize(bRaw)) return 1.0
        if (aTokens.isEmpty() || bTokens.isEmpty()) return 0.0
        val intersection = aTokens.intersect(bTokens).size.toDouble()
        val union = aTokens.union(bTokens).size.toDouble()
        return if (union == 0.0) 0.0 else intersection / union
    }

    private fun normalize(name: String): String {
        var n = name.trim().lowercase()
        NAME_NOISE.forEach { n = n.replace(it, " ") }
        return n.replace(Regex("[\\s\\p{Punct}　]+"), "").trim()
    }

    private fun tokenize(name: String): Set<String> {
        var n = name.trim().lowercase()
        NAME_NOISE.forEach { n = n.replace(it, " ") }
        return n.split(Regex("[\\s\\p{Punct}　]+"))
            .map { it.trim() }
            .filter { it.length > 1 }
            .toSet()
    }
}
