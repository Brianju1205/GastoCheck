package com.example.gastocheck.data.gemini

import com.google.gson.annotations.SerializedName

// ===============================
// REQUEST A GEMINI
// ===============================
data class GeminiRequest(
    val contents: List<Content>,
    //val generationConfig: GenerationConfig = GenerationConfig()
)

data class Content(
    val role: String = "user",
    val parts: List<Part>
)

data class Part(
    val text: String
)



// ===============================
// RESPONSE DE GEMINI
// ===============================
data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?
)

// ===============================
// MODELO EXISTENTE: TRANSACCIÓN
// ===============================
data class TransaccionInterpretada(
    val tipo: String,
    val monto: Double,
    val categoria: String,
    val descripcion: String,
    val cuenta_origen: String = "Efectivo",
    val cuenta_destino: String? = null
)

// ===============================
// MODELO NUEVO: ANÁLISIS FINANCIERO
// ===============================
data class AnalisisFinancieroResponse(
    @SerializedName("proyeccion_fin_mes")
    val proyeccionFinMes: Double,

    @SerializedName("tendencia_texto")
    val tendenciaTexto: String, // Ej: "Crecimiento", "Estable", "Riesgo"

    @SerializedName("patrones")
    val patrones: List<String>, // Ej: ["Gastas más los viernes", "Suscripciones altas"]

    @SerializedName("insight_principal")
    val insightPrincipal: String,

    @SerializedName("recomendacion_accionable")
    val recomendacion: String
)
