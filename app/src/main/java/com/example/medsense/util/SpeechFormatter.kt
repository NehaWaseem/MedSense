package com.example.medsense.util

import com.example.medsense.domain.MedicineInfo

object SpeechFormatter {

    fun formatForSpeech(medicine: MedicineInfo): String {
        if (!medicine.isConfident) {
            return "Possible match: ${medicine.name}. Please rescan the front label for confirmation."
        }

        val cleanName = medicine.name.cleanText()
        val cleanStrength = medicine.strength.cleanText()
        
        // Extract strongest dosage phrase (max 1 sentence, ~20 words)
        val dosage = medicine.dosage
            .cleanText()
            .splitToSentences()
            .firstOrNull { it.length > 10 } // Look for a substantial sentence
            ?.truncateToWords(24) ?: "Consult packaging for dosage"

        // Extract single most important warning (max 1 sentence, ~15 words)
        val warning = medicine.warnings
            .cleanText()
            .splitToSentences()
            .filter { s -> 
                val lower = s.lowercase()
                lower.contains("do not") || lower.contains("stop") || lower.contains("danger") || lower.contains("risk")
            }
            .firstOrNull()
            ?.truncateToWords(16) ?: "Read all warnings before use"

        // Deterministic Template: [Name] [Strength]. Usual dose: [Dosage]. Warning: [Warning].
        return "Detected: $cleanName $cleanStrength. Usual dose: $dosage. Warning: $warning."
            .replace(Regex("(?i)warning warning"), "Warning")
            .replace(Regex("(?i)dosage dosage"), "Dosage")
            .replace(Regex("\\s+"), " ")
    }

    private fun String.cleanText(): String {
        return this.replace(Regex("<[^>]*>"), "")
            .replace(Regex("[#*_]"), "") // Remove markdown-style artifacts
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.splitToSentences(): List<String> {
        // Splits by punctuation followed by space, or common drug label delimiters
        return this.split(Regex("(?<=[.!?])\\s+|(?<=[;])\\s+"))
            .map { it.trim() }
            .filter { it.length > 5 }
    }

    private fun String.truncateToWords(limit: Int): String {
        val words = this.split(" ")
        if (words.size <= limit) return this
        return words.take(limit).joinToString(" ") + "."
    }
}
