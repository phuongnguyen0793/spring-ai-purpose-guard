package com.example.springai.training

import com.example.springai.config.TrainingProperties
import com.example.springai.domain.Relation
import com.example.springai.util.Csv
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.system.exitProcess

// OFFLINE step. Reads seed rows (industry_id, business_type, company_name),
// asks the LLM to classify the relation, and appends the results to the trained
// CSV as a NEW data version. Runs only when app.training.enabled=true, then exits.
//
//   ./gradlew bootRun --args="--app.training.enabled=true"
@Component
@ConditionalOnProperty(prefix = "app.training", name = ["enabled"], havingValue = "true")
class CompanyNameRelationTrainer(
    private val chatClient: ChatClient,
    private val trainingProps: TrainingProperties,
    private val context: ConfigurableApplicationContext
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val header = listOf("industry_id", "business_type", "company_name", "relation", "data_version")

    override fun run(vararg args: String?) {
        val seedPath = Path.of(trainingProps.seedCsvPath)
        val outputPath = Path.of(trainingProps.outputCsvPath)

        if (!Files.exists(seedPath)) {
            logger.error("Seed CSV not found at '{}' — nothing to train", seedPath.toAbsolutePath())
            exit(1)
            return
        }

        val seeds = readSeeds(seedPath)
        if (seeds.isEmpty()) {
            logger.warn("Seed CSV '{}' had no data rows", seedPath)
            exit(0)
            return
        }

        val nextVersion = nextDataVersion(outputPath)
        logger.info("Training {} seed rows into data version {}", seeds.size, nextVersion)

        val rows = seeds.map { seed ->
            val relation = classify(seed.industryId, seed.businessType, seed.companyName)
            logger.info(
                "  [{}] {}/{} -> {}",
                relation, seed.industryId, seed.businessType, seed.companyName
            )
            listOf(seed.industryId, seed.businessType, seed.companyName, relation.name, nextVersion.toString())
        }

        writeOutput(outputPath, rows)
        logger.info("Wrote {} trained rows to '{}' (data version {})", rows.size, outputPath.toAbsolutePath(), nextVersion)
        logger.info("Training complete. Start the app normally to load version {} into MySQL.", nextVersion)
        exit(0)
    }

    private data class Seed(val industryId: String, val businessType: String, val companyName: String)

    private fun readSeeds(path: Path): List<Seed> {
        val lines = Files.readAllLines(path).filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val head = Csv.parseLine(lines.first()).map { it.trim().lowercase() }
        val iInd = head.indexOf("industry_id")
        val iBiz = head.indexOf("business_type")
        val iName = head.indexOf("company_name")
        if (iInd < 0 || iBiz < 0 || iName < 0) {
            logger.error("Seed CSV header must contain industry_id, business_type, company_name. Found: {}", head)
            return emptyList()
        }
        return lines.drop(1).mapNotNull { line ->
            val cols = Csv.parseLine(line)
            runCatching { Seed(cols[iInd].trim(), cols[iBiz].trim(), cols[iName].trim()) }.getOrNull()
        }
    }

    private fun classify(industryId: String, businessType: String, companyName: String): Relation {
        val system = """
            You classify whether a company name is appropriate for a given industry and business type.
            Answer with EXACTLY ONE WORD, one of: CONSISTENT, INCONSISTENT, NEUTRAL.
            CONSISTENT   = the name clearly fits the industry/business type.
            INCONSISTENT = the name clearly belongs to a different, conflicting domain.
            NEUTRAL      = generic name that neither fits nor conflicts.
        """.trimIndent()
        val user = """
            Industry id: $industryId
            Business type: $businessType
            Company name: $companyName
            Answer:
        """.trimIndent()

        return runCatching {
            val answer = chatClient.prompt()
                .system(system)
                .user(user)
                .call()
                .content()
                ?.trim()
                ?.uppercase()
                ?: ""
            val token = Regex("CONSISTENT|INCONSISTENT|NEUTRAL").find(answer)?.value ?: "NEUTRAL"
            Relation.fromString(token)
        }.getOrElse {
            logger.warn("LLM classification failed for '{}' — defaulting to NEUTRAL: {}", companyName, it.message)
            Relation.NEUTRAL
        }
    }

    private fun nextDataVersion(outputPath: Path): Int {
        if (!Files.exists(outputPath)) return 1
        val lines = Files.readAllLines(outputPath).filter { it.isNotBlank() }
        if (lines.size <= 1) return 1
        val head = Csv.parseLine(lines.first()).map { it.trim().lowercase() }
        val vIdx = head.indexOf("data_version")
        if (vIdx < 0) return 1
        val maxVersion = lines.drop(1)
            .mapNotNull { Csv.parseLine(it).getOrNull(vIdx)?.trim()?.toIntOrNull() }
            .maxOrNull() ?: 0
        return maxVersion + 1
    }

    private fun writeOutput(outputPath: Path, rows: List<List<String>>) {
        outputPath.parent?.let { Files.createDirectories(it) }
        if (!Files.exists(outputPath)) {
            val content = buildString {
                appendLine(Csv.row(header))
                rows.forEach { appendLine(Csv.row(it)) }
            }
            Files.writeString(outputPath, content)
        } else {
            val content = buildString { rows.forEach { appendLine(Csv.row(it)) } }
            Files.writeString(outputPath, content, StandardOpenOption.APPEND)
        }
    }

    private fun exit(code: Int) {
        val springExit = SpringApplication.exit(context, ExitCodeGenerator { code })
        exitProcess(springExit)
    }
}
