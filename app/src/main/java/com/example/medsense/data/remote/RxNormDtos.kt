package com.example.medsense.data.remote

data class RxNormResponse(
    val drugGroup: DrugGroup? = null
)

data class DrugGroup(
    val name: String? = null,
    val conceptGroup: List<ConceptGroup>? = null
)

data class ConceptGroup(
    val tty: String? = null,
    val conceptProperties: List<ConceptProperty>? = null
)

data class ConceptProperty(
    val rxcui: String? = null,
    val name: String? = null,
    val synonym: String? = null,
    val tty: String? = null,
    val language: String? = null
)

data class RxNormApproximateResponse(
    val approximateGroup: ApproximateGroup? = null
)

data class ApproximateGroup(
    val candidate: List<RxNormCandidate>? = null
)

data class RxNormCandidate(
    val rxcui: String? = null,
    val score: String? = null
)

data class RxNormPropertiesResponse(
    val properties: ConceptProperty? = null
)

data class RxNormAllRelatedResponse(
    val allRelatedGroup: AllRelatedGroup? = null
)

data class AllRelatedGroup(
    val conceptGroup: List<ConceptGroup>? = null
)
