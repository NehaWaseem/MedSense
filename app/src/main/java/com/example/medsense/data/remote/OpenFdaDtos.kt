package com.example.medsense.data.remote

data class OpenFdaDrugLabelResponse(
    val results: List<OpenFdaDrugLabelResult>? = null
)

data class OpenFdaDrugLabelResult(
    val openfda: OpenFdaOpenFda? = null,
    val warnings: List<String>? = null,
    val boxed_warning: List<String>? = null,
    val dosage_and_administration: List<String>? = null,
    val indications_and_usage: List<String>? = null
)

data class OpenFdaOpenFda(
    val brand_name: List<String>? = null,
    val generic_name: List<String>? = null,
    val substance_name: List<String>? = null
)

