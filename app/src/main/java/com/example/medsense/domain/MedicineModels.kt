package com.example.medsense.domain

data class MedicineInfo(
    val name: String,
    val strengthMg: String,
    val recommendedDosage: String,
    val warnings: String
)

interface MedicineRepository {
    suspend fun findByText(recognizedText: String): MedicineInfo?
}

class InMemoryMedicineRepository : MedicineRepository {

    private val medicines: List<MedicineInfo> = listOf(
        MedicineInfo(
            name = "Lisinopril",
            strengthMg = "20 mg",
            recommendedDosage = "Take 20 mg once daily, with or without food.",
            warnings = "May cause dizziness, cough, or kidney issues. Do not use during pregnancy. Monitor blood pressure and kidney function regularly."
        ),
        MedicineInfo(
            name = "Paracetamol",
            strengthMg = "500 mg",
            recommendedDosage = "Take 500–1000 mg every 4–6 hours as needed. Do not exceed 4000 mg per day.",
            warnings = "Overdose can cause serious liver damage. Avoid combining with other acetaminophen-containing products or heavy alcohol use."
        )
    )

    override suspend fun findByText(recognizedText: String): MedicineInfo? {
        val textLower = recognizedText.lowercase()
        return medicines.firstOrNull { med ->
            val nameMatch = textLower.contains(med.name.lowercase())
            val strengthMatch = med.strengthMg.isNotBlank() &&
                textLower.contains(med.strengthMg.lowercase().replace(" ", ""))
            nameMatch || strengthMatch
        }
    }
}

