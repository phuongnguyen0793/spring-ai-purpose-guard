package com.example.springai.util

import org.springframework.stereotype.Component

// Lightweight rule-based validator for Japanese company names (MVP).
// Rules implemented here are illustrative; adapt to legal requirements.

@Component
class JapaneseCompanyNameValidator {

    fun isValid(name: String): Boolean {
        val reasons = validateReasons(name)
        return reasons.isEmpty()
    }

    fun validateReasons(name: String): List<String> {
        val r = mutableListOf<String>()
        val trimmed = name.trim()
        if (trimmed.isEmpty()) r.add("Name must not be empty")
        if (trimmed.length > 100) r.add("Name is too long (max 100 characters)")
        // Block path separators and control whitespace — regular spaces are allowed.
        val illegalChars = setOf('\\', '/', '\n', '\r', '\t')
        if (trimmed.any { it in illegalChars }) {
            r.add("Name contains illegal whitespace or slash characters")
        }
        // Basic kanji/hiragana/katakana/ASCII check: require at least one Japanese or ASCII character
        val jpPattern = Regex("[\\u3000-\\u30FF\\u4E00-\\u9FFF]")
        val asciiPattern = Regex("[-A-Za-z0-9&,. ]")
        if (!jpPattern.containsMatchIn(trimmed) && !asciiPattern.containsMatchIn(trimmed)) {
            r.add("Name must contain Japanese or ASCII characters")
        }
        // Prevent using reserved words like "株式会社" in the middle (simple rule)
        if (trimmed.contains("株式会社") && !trimmed.startsWith("株式会社")) r.add("If using '株式会社' it must be a prefix")
        return r
    }
}
