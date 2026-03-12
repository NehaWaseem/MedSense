package com.example.medsense.data.remote

import android.util.Log
import com.example.medsense.domain.MedicineInfo
import com.example.medsense.domain.MedicineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteMedicineRepository(
    private val openFdaApi: OpenFdaDrugLabelApi
) : MedicineRepository {

    override suspend fun findByText(recognizedText: String): MedicineInfo? = withContext(Dispatchers.IO) {
        Log.d("RemoteRepo", "Scanning OCR: \n$recognizedText")

        // 1. Get potential candidates (prioritizing the largest text/lines)
        val candidates = extractCleanCandidates(recognizedText)
        Log.d("RemoteRepo", "Candidates to check: $candidates")

        for (term in candidates) {
            try {
                // 2. Search OpenFDA for this specific term
                val response = try {
                    openFdaApi.searchDrugLabels(search = "openfda.brand_name:\"$term\" OR openfda.generic_name:\"$term\"", limit = 1)
                } catch (e: Exception) { null }

                val result = response?.results?.firstOrNull() ?: continue

                // 3. Validation: Does the result actually match our term? 
                // (Prevents "Adults" matching a random med)
                val brandNames = result.openfda?.brand_name?.map { it.lowercase() } ?: emptyList()
                val genericNames = result.openfda?.generic_name?.map { it.lowercase() } ?: emptyList()
                
                if (brandNames.none { it.contains(term.lowercase()) } && 
                    genericNames.none { it.contains(term.lowercase()) }) {
                    continue 
                }

                // 4. Success! Extract info
                val finalName = result.openfda?.brand_name?.firstOrNull() ?: term
                val dosage = (result.dosage_and_administration?.firstOrNull() 
                    ?: result.indications_and_usage?.firstOrNull() 
                    ?: "Check packaging for instructions.").cleanAndShorten(200)

                val warnings = (result.boxed_warning?.firstOrNull() 
                    ?: result.warnings?.firstOrNull() 
                    ?: "Check packaging for warnings.").cleanAndShorten(200)

                return@withContext MedicineInfo(
                    name = finalName,
                    strengthMg = extractStrength(recognizedText) ?: "",
                    recommendedDosage = dosage,
                    warnings = warnings
                )
            } catch (e: Exception) {
                Log.e("RemoteRepo", "Error searching for '$term'", e)
            }
        }
        null
    }

    private fun String.cleanAndShorten(limit: Int): String {
        val clean = this.replace(Regex("<[^>]*>"), "").replace(Regex("\\s+"), " ").trim()
        if (clean.length <= limit) return clean
        val truncated = clean.take(limit)
        val lastDot = truncated.lastIndexOf('.')
        return if (lastDot > limit / 2) truncated.substring(0, lastDot + 1) else "$truncated..."
    }
}

private fun extractCleanCandidates(text: String): List<String> {
    val ignore = setOf("tablet", "tablets", "capsule", "mg", "ml", "dosage", "adults", "daily", "relief", "supplement")
    return text.split("\n")
        .map { it.trim().replace(Regex("[^A-Za-z0-9 ]"), "") }
        .filter { it.length >= 3 && it.lowercase() !in ignore }
}

private fun extractStrength(text: String): String? {
    val m = Regex("(\\d{1,4})\\s*(mg|mcg|g|ml)\\b", RegexOption.IGNORE_CASE).find(text)
    return m?.value
}
