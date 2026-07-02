package com.example.springai.startup

import com.example.springai.config.RuleProperties
import com.example.springai.domain.CompanyNameRule
import com.example.springai.domain.Relation
import com.example.springai.repository.CompanyNameRuleRepository
import com.example.springai.repository.DataVersionRepository
import com.example.springai.service.CompanyNameValidationService.Companion.RULE_DATASET
import com.example.springai.util.Csv
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

// On startup: read the trained CSV and import only rows whose data_version has
// not yet been loaded into MySQL. Each imported version is recorded in the
// data_version table for incremental, versioned loading.
@Component
@Order(10)
class RuleDataLoader(
    private val ruleRepository: CompanyNameRuleRepository,
    private val dataVersionRepository: DataVersionRepository,
    private val ruleProps: RuleProperties
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) {
        val path = Path.of(ruleProps.csvPath)
        if (!Files.exists(path)) {
            logger.warn("Rule CSV not found at '{}' — skipping rule import", path.toAbsolutePath())
            return
        }

        val parsed = parseCsv(path)
        if (parsed.isEmpty()) {
            logger.info("Rule CSV '{}' contained no data rows", path)
            return
        }

        val alreadyLoaded = dataVersionRepository.loadedVersions(RULE_DATASET)
        val newRowsByVersion = parsed
            .filter { it.dataVersion !in alreadyLoaded }
            .groupBy { it.dataVersion }

        if (newRowsByVersion.isEmpty()) {
            logger.info("No new rule data versions to load (loaded versions: {})", alreadyLoaded.sorted())
            return
        }

        newRowsByVersion.toSortedMap().forEach { (version, rows) ->
            ruleRepository.insertBatch(rows)
            dataVersionRepository.record(RULE_DATASET, version, rows.size)
            logger.info("Imported {} company-name rules for data version {}", rows.size, version)
        }
    }

    private fun parseCsv(path: Path): List<CompanyNameRule> {
        val lines = Files.readAllLines(path).filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val header = Csv.parseLine(lines.first()).map { it.trim().lowercase() }
        val idx = mapOf(
            "industry_id" to header.indexOf("industry_id"),
            "business_type" to header.indexOf("business_type"),
            "company_name" to header.indexOf("company_name"),
            "relation" to header.indexOf("relation"),
            "data_version" to header.indexOf("data_version")
        )
        if (idx.values.any { it < 0 }) {
            logger.error("Rule CSV header missing required columns. Found: {}", header)
            return emptyList()
        }

        return lines.drop(1).mapNotNull { line ->
            val cols = Csv.parseLine(line)
            runCatching {
                CompanyNameRule(
                    id = 0,
                    industryId = cols[idx.getValue("industry_id")].trim(),
                    businessType = cols[idx.getValue("business_type")].trim(),
                    companyName = cols[idx.getValue("company_name")].trim(),
                    relation = Relation.fromString(cols[idx.getValue("relation")]),
                    dataVersion = cols[idx.getValue("data_version")].trim().toInt()
                )
            }.getOrElse {
                logger.warn("Skipping malformed rule CSV line: {}", line)
                null
            }
        }
    }
}
