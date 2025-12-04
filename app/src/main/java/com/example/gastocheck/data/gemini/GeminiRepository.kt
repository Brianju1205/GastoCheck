package com.example.gastocheck.data.gemini

import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

// 1. ACTUALIZAMOS LA ESTRUCTURA DE DATOS
// Ahora incluye cuentas para soportar transferencias y lógica avanzada

class GeminiRepository @Inject constructor() {

    private val apiKey = "AIzaSyDdWq_ThblXwWLz7Gv4U-imbmfxvvHvJ-g" // Tu API Key

    private val api = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApi::class.java)

    suspend fun interpretarTexto(texto: String): TransaccionInterpretada? {
        // 2. PROMPT AVANZADO (Ingeniería de Prompts)
        val prompt = """
            Eres un asistente financiero experto. Analiza el texto: "$texto".
            
            Tu objetivo es extraer datos estructurados para una App de finanzas.
            
            REGLAS DE ANÁLISIS:
            1. **Tipo**: 
               - Si dice "transferir", "pasar", "mover", "enviar" -> "TRANSFERENCIA".
               - Si dice "ahorrar", "meta", "guardar para" -> "META".
               - Si implica entrada de dinero ("gané", "recibí", "depósito") -> "INGRESO".
               - Cualquier otra salida de dinero -> "GASTO".
            
            2. **Cuentas (Origen y Destino)**:
               - Busca nombres de cuentas como: "Banco", "Tarjeta", "Nomina", "Ahorros", "BBVA", "Santander".
               - Regla de Oro: Si NO se menciona ninguna cuenta, asume "Efectivo".
               - Si es GASTO: 'cuenta_origen' es la cuenta usada.
               - Si es INGRESO: 'cuenta_origen' es donde entra el dinero.
               - Si es TRANSFERENCIA: Identifica 'cuenta_origen' (desde) y 'cuenta_destino' (hacia).
            
            3. **Monto**: Extrae el número. Si hay varios, usa el que parezca el total.
            
            4. **Categoría**: Clasifica en una sola palabra (ej. Comida, Transporte, Salud, Ocio, Servicios, Casa).
            
            5. **Descripción**: Resumen muy breve (máx 5 palabras).

            Responde ÚNICAMENTE con este JSON:
            {
              "tipo": "GASTO", 
              "monto": 0.0,
              "categoria": "Otros",
              "descripcion": "Texto breve",
              "cuenta_origen": "Efectivo",
              "cuenta_destino": null
            }
        """.trimIndent()

        val request = GeminiRequest(listOf(Content(listOf(Part(prompt)))))

        return try {
            val response = api.generarContenido(apiKey, request)
            val jsonString = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (jsonString != null) {
                val jsonLimpio = jsonString.replace("```json", "").replace("```", "").trim()
                Gson().fromJson(jsonLimpio, TransaccionInterpretada::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}