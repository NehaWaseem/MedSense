package com.example.medsense.data.remote

import android.util.Log
import com.example.medsense.domain.MedicineInfo
import com.example.medsense.domain.MedicineRepository
import com.example.medsense.ml.OcrCandidate
import com.example.medsense.ml.OcrResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteMedicineRepository(
    private val openFdaApi: OpenFdaDrugLabelApi
) : MedicineRepository {

    override suspend fun findByText(recognizedText: String): MedicineInfo? {
        return null 
    }

    suspend fun findByOcrResult(ocrResult: OcrResult): MedicineInfo? = withContext(Dispatchers.IO) {
        val candidates = ocrResult.candidates
            .filter { isValidCandidate(it) }
            .sortedByDescending { calculateCandidateScore(it) }
            .take(5)

        // Medicine-likeness gate
        val hasMedContext = containsMedicineContext(ocrResult.fullText)
        if (candidates.isEmpty() && !hasMedContext) {
            return@withContext null
        }

        for (candidate in candidates) {
            val term = candidate.text.clean()
            if (term.length < 3) continue

            try {
                val response = openFdaApi.searchDrugLabels(
                    search = "openfda.brand_name:\"$term\" OR openfda.generic_name:\"$term\"",
                    limit = 1
                )
                val result = response.results?.firstOrNull() ?: continue

                val brandNames = result.openfda?.brand_name ?: emptyList()
                val genericNames = result.openfda?.generic_name ?: emptyList()
                
                val nameMatchScore = calculateNameMatchScore(term, brandNames, genericNames)
                if (nameMatchScore < 0.65f) continue 

                val strength = extractStrength(ocrResult.fullText) ?: ""
                
                // Refined Confidence Scoring
                val finalScore = (nameMatchScore * 0.40f) + 
                                (candidate.confidence * 0.20f) + 
                                (if (strength.isNotEmpty()) 0.15f else 0f) +
                                (if (candidate.isCenter) 0.15f else 0f) +
                                (if (hasMedContext) 0.10f else 0f)

                // Lowered threshold for better flexibility
                val isConfident = finalScore >= 0.72f

                return@withContext MedicineInfo(
                    name = brandNames.firstOrNull() ?: term,
                    strength = strength,
                    dosage = result.dosage_and_administration?.firstOrNull() ?: result.indications_and_usage?.firstOrNull() ?: "",
                    warnings = result.boxed_warning?.firstOrNull() ?: result.warnings?.firstOrNull() ?: "",
                    confidenceScore = finalScore,
                    isConfident = isConfident
                )
            } catch (e: Exception) {
                Log.e("RemoteRepo", "Search failed for $term", e)
            }
        }
        null
    }

    private fun isValidCandidate(c: OcrCandidate): Boolean {
        val text = c.text.lowercase()
        val noise = setOf("warning", "dosage", "drug facts", "keep out of reach", "active ingredient", "ingredients", "label", "medicine")
        return text.length >= 3 && noise.none { text.contains(it) }
    }

    private fun calculateCandidateScore(c: OcrCandidate): Float {
        var score = c.confidence * 0.4f
        if (c.isCenter) score += 0.4f 
        if (c.boxArea > 3000) score += 0.2f 
        return score
    }

    private fun calculateNameMatchScore(term: String, brands: List<String>, generics: List<String>): Float {
        val t = term.lowercase()
        val allNames = (brands + generics).map { it.lowercase() }
        if (allNames.any { it == t }) return 1.0f
        if (allNames.any { it.contains(t) || t.contains(it) }) return 0.85f
        return 0f
    }

    private fun containsMedicineContext(text: String): Boolean {
        val contextTokens = listOf("mg", "mcg", "ml", "tablet", "capsule", "caplet", "softgel", "dose", "facts")
        return contextTokens.any { text.lowercase().contains(it) }
    }

    private fun String.clean() = this.replace(Regex("[^A-Za-z0-9 ]"), "").trim()

    private fun extractStrength(text: String): String? {
        val m = Regex("(\\d{1,4})\\s*(mg|mcg|g|ml)\\b", RegexOption.IGNORE_CASE).find(text)
        return m?.value
    }
}
