package com.example.gastocheck.data.gemini

import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject


class GeminiRepository @Inject constructor() {


    private val apiKey = "AIzaSyDdWq_ThblXwWLz7Gv4U-imbmfxvvHvJ-g"

    private val api = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApi::class.java)

    suspend fun interpretarTexto(texto: String): TransaccionInterpretada? {
        // Instrucciones para la IA
        val prompt = """
            Analiza el siguiente texto financiero: "$texto".
            Tu trabajo es extraer la información estructurada.
            
            Reglas:
            1. Identifica si es "GASTO", "INGRESO" o "META".
            2. Extrae el monto numérico (ej. 500).
            3. Asigna una categoría corta (ej. Comida, Transporte, Salud, Ropa, Ocio).
            4. Genera una descripción corta (máx 5 palabras).
            
            Responde SOLO con este formato JSON exacto:
            {
              "tipo": "GASTO", 
              "monto": 0.0,
              "categoria": "General",
              "descripcion": "Descripción corta"
            }
        """.trimIndent()
        
        val request = GeminiRequest(listOf(Content(listOf(Part(prompt)))))

        return try {
            val response = api.generarContenido(apiKey, request)
            val jsonString = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (jsonString != null) {
                // Limpiamos el JSON por si la IA añade bloques de código markdown
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