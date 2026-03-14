package com.example.medsense.domain

data class MedicineInfo(
    val name: String,
    val strength: String,
    val dosage: String,
    val warnings: String,
    val confidenceScore: Float = 1.0f,
    val isConfident: Boolean = true
)

interface MedicineRepository {
    suspend fun findByText(recognizedText: String): MedicineInfo?
}
