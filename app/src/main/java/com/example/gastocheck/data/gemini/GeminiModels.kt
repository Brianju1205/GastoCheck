package com.example.gastocheck.data.gemini

data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig = GenerationConfig()
)

data class Content(val parts: List<Part>)
data class Part(val text: String)

data class GenerationConfig(
    val responseMimeType: String = "application/json"
)

data class GeminiResponse(val candidates: List<Candidate>?)
data class Candidate(val content: Content?)

// ESTRUCTURA FINAL DE LA TRANSACCIÓN INTERPRETADA
data class TransaccionInterpretada(
    val tipo: String,           // "GASTO", "INGRESO", "TRANSFERENCIA", "META"
    val monto: Double,
    val categoria: String,
    val descripcion: String,
    val cuenta_origen: String = "Efectivo", // Por defecto Efectivo
    val cuenta_destino: String? = null      // Solo para transferencias
)