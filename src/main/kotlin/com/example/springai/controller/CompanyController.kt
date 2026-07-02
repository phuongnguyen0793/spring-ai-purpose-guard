package com.example.springai.controller

import com.example.springai.service.CompanyNameValidationService
import com.example.springai.service.PurposeSearchService
import com.example.springai.web.dto.AddPurposeRequest
import com.example.springai.web.dto.PurposeRequest
import com.example.springai.web.dto.ValidateNameRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class CompanyController(
    private val validationService: CompanyNameValidationService,
    private val purposeSearchService: PurposeSearchService
) {

    // Rule-based, data-driven validation using trained (industry, business type,
    // company name, relation) data loaded from CSV into MySQL.
    @PostMapping("/validate-name")
    fun validateName(@RequestBody req: ValidateNameRequest): ResponseEntity<Any> {
        val result = validationService.validate(
            companyName = req.companyName,
            industryId = req.industryId,
            businessType = req.businessType
        )
        return ResponseEntity.ok(result)
    }

    // Semantic search backed by the in-memory vector store (loaded from MySQL).
    @PostMapping("/search-purpose")
    fun searchPurpose(@RequestBody req: PurposeRequest): ResponseEntity<Any> {
        val results = purposeSearchService.search(
            purposeText = req.purpose,
            topK = req.topK,
            threshold = req.threshold
        )
        return ResponseEntity.ok(
            mapOf(
                "query" to req.purpose,
                "topK" to req.topK,
                "threshold" to req.threshold,
                "results" to results,
                "count" to results.size
            )
        )
    }

    // Persist a new purpose to MySQL and index it in memory (immediately searchable).
    @PostMapping("/purposes")
    fun addPurpose(@RequestBody req: AddPurposeRequest): ResponseEntity<Any> {
        val created = purposeSearchService.addPurpose(req.purpose)
        return ResponseEntity.ok(created)
    }
}
