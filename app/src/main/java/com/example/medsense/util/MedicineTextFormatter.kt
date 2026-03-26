package com.example.medsense.util

import com.example.medsense.domain.MedicineInfo

object MedicineTextFormatter {

    /**
     * Formats medicine details into a natural, concise summary for Text-to-Speech.
     */
    fun formatForSpeech(medicine: MedicineInfo): String {
        if (!medicine.isConfident) {
            return "Possible match: ${medicine.name}. Please rescan the front label for confirmation. I am ready for the next command."
        }

        val name = medicine.name.clean()
        val strength = medicine.strength.clean()
        
        val dosageSummary = summarizeText(medicine.dosage, listOf("take", "tablet", "dose", "adults", "children"), 30)
            ?: "Please consult the packaging for dosage instructions."

        val warningSummary = summarizeText(medicine.warnings, listOf("do not", "stop", "allergy", "warning", "if"), 25)
            ?: "Read all warnings on the label before use."

        return "Detected: $name $strength. $dosageSummary $warningSummary. I am ready for the next command."
    }

    /**
     * Formats long technical text into a readable, punctuated list for the UI.
     */
    fun formatForDisplay(text: String): String {
        val clean = text.clean()
        if (clean.isBlank()) return "Information not available."

        val sentences = splitToSentences(clean)
            .filter { it.length > 5 && !isStandaloneHeader(it) }
            .map { it.replaceFirstChar { char -> char.uppercase() } }

        return if (sentences.size > 1) {
            sentences.joinToString("\n\n• ", prefix = "• ")
        } else {
            sentences.firstOrNull() ?: clean
        }
    }

    private fun isStandaloneHeader(text: String): Boolean {
        val headers = setOf("warning", "warnings", "directions", "dosage", "indications", "usage", "active ingredient", "other information")
        val clean = text.lowercase().trim().removeSuffix(":").removeSuffix(".")
        return headers.contains(clean)
    }

    private fun summarizeText(text: String, keywords: List<String>, wordLimit: Int): String? {
        val cleanText = text.clean()
        if (cleanText.isBlank()) return null
        
        val sentences = splitToSentences(cleanText)
            .filter { !isStandaloneHeader(it) }
        
        // Find the best sentence containing key information
        val bestSentence = sentences.firstOrNull { sentence ->
            val lower = sentence.lowercase()
            keywords.any { lower.contains(it) }
        } ?: sentences.firstOrNull { it.length > 15 }

        return bestSentence?.truncateToWords(wordLimit)?.let { 
            if (!it.endsWith(".")) "$it." else it 
        }
    }

    private fun String.clean(): String {
        return this.replace(Regex("<[^>]*>"), "") // Remove HTML
            .replace(Regex("[#*_]"), "")         // Remove Markdown
            .replace(Regex("\\s+"), " ")         // Normalize whitespace
            .trim()
    }

    private fun splitToSentences(text: String): List<String> {
        val markers = listOf(
            "Adults", "Children", "Take", "Do not", "If", "Stop", "Ask", 
            "Directions", "Warning", "Symptoms", "The", "Use", "Every", "When"
        )
        
        var refined = text
        markers.forEach { marker ->
            refined = refined.replace(Regex("(?<=[^.!?])\\s+($marker\\b)"), ". $1")
        }

        return refined.split(Regex("(?<=[.!?])\\s+|(?<=:)\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun String.truncateToWords(limit: Int): String {
        val words = this.split(" ")
        if (words.size <= limit) return this
        return words.take(limit).joinToString(" ") + "..."
    }
}
