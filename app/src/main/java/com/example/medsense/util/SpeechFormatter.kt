package com.example.medsense.util

import com.example.medsense.domain.MedicineInfo

object SpeechFormatter {

    fun formatForSpeech(medicine: MedicineInfo): String {
        if (!medicine.isConfident) {
            return "Possible match: ${medicine.name}. Please rescan the front label for confirmation."
        }

        val namePart = "Detected: ${medicine.name} ${medicine.strength}."
        
        val dosagePart = medicine.dosage
            .cleanText()
            .splitToSentences()
            .firstOrNull()?.let { "Usual dose: $it" } ?: ""

        val warningPart = medicine.warnings
            .cleanText()
            .splitToSentences()
            .filter { it.length > 5 }
            .distinct()
            .take(2)
            .joinToString(". ")
            .let { if (it.isNotBlank()) "Warning: $it" else "" }

        return listOf(namePart, dosagePart, warningPart)
            .filter { it.isNotBlank() }
            .joinToString(". ")
            .replace(Regex("(?i)warning warning"), "Warning")
            .replace(Regex("(?i)dosage dosage"), "Dosage")
    }

    private fun String.cleanText(): String {
        return this.replace(Regex("<[^>]*>"), "") // Remove HTML
            .replace(Regex("\\s+"), " ")          // Normalize spaces
            .trim()
    }

    private fun String.splitToSentences(): List<String> {
        return this.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
