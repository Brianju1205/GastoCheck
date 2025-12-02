package com.example.gastocheck.data.gemini

data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig = GenerationConfig()
)

data class Content(val parts: List<Part>)
data class Part(val text: String)

data class GenerationConfig(
    val responseMimeType: String = "application/json" // Forzamos respuesta JSON
)

data class GeminiResponse(val candidates: List<Candidate>?)
data class Candidate(val content: Content?)

// Esta es la estructura limpia que usará tu App
data class TransaccionInterpretada(
    val tipo: String,      // "GASTO", "INGRESO", "META"
    val monto: Double,
    val categoria: String,
    val descripcion: String
)