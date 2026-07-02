package com.example.springai.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JapaneseCompanyNameValidatorTest {

    private val validator = JapaneseCompanyNameValidator()

    @Test
    fun `allows ASCII company names with spaces`() {
        assertTrue(validator.isValid("Tech Solutions Inc"))
    }

    @Test
    fun `rejects slash characters`() {
        assertFalse(validator.isValid("Bad/Name"))
    }

    @Test
    fun `rejects empty name`() {
        assertFalse(validator.isValid(""))
    }
}
