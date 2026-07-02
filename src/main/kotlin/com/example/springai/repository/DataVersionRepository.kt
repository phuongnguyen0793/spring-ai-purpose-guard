package com.example.springai.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class DataVersionRepository(private val jdbc: JdbcTemplate) {

    fun loadedVersions(dataset: String): Set<Int> =
        jdbc.queryForList("SELECT version FROM data_version WHERE dataset = ?", Int::class.java, dataset)
            .toSet()

    fun maxVersion(dataset: String): Int? =
        jdbc.queryForObject("SELECT MAX(version) FROM data_version WHERE dataset = ?", Int::class.java, dataset)

    fun record(dataset: String, version: Int, rowCount: Int) {
        jdbc.update(
            "INSERT INTO data_version (dataset, version, row_count) VALUES (?, ?, ?)",
            dataset,
            version,
            rowCount
        )
    }
}
