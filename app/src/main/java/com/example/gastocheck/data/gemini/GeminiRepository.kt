package com.example.gastocheck.data.gemini

import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

class GeminiRepository @Inject constructor() {

    // Asegúrate de que tu API Key sea válida
    private val apiKey = "AIzaSyDdWq_ThblXwWLz7Gv4U-imbmfxvvHvJ-g"

    private val api = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApi::class.java)

    suspend fun interpretarTexto(texto: String): TransaccionInterpretada? {
        // --- MEJORA DEL PROMPT ---
        val prompt = """
            Eres un asistente financiero inteligente. Tu trabajo es extraer datos de una frase dicha por el usuario para una App de Finanzas.
            
            Frase del usuario: "$texto"
            
            INSTRUCCIONES DE ANÁLISIS:
            1. **Tipo**:
               - Si la frase menciona "transferir", "pasar a", "enviar a", "moví a" o implica movimiento entre dos cuentas propias (ej: "de Bancomer a Efectivo"), el tipo ES OBLIGATORIAMENTE "TRANSFERENCIA".
               - Si dice "ahorrar", "guardar para", "meta" -> "META".
               - Si recibí dinero -> "INGRESO".
               - Si gasté dinero -> "GASTO".
            
            2. **Cuentas**:
               - Detecta nombres de cuentas (ej: Efectivo, Bancomer, BBVA, Santander, Nomina, Ahorros, Tarjeta, Nu).
               - Si es "TRANSFERENCIA": Debes identificar AMBAS. 'cuenta_origen' (de donde sale) y 'cuenta_destino' (a donde llega).
               - Si es GASTO/INGRESO: Solo identifica 'cuenta_origen'.
            
            3. **Monto**: Extrae el número principal.
            
            4. **Descripción**: Un resumen muy breve (máx 5 palabras). Si no hay detalle, usa "Transferencia".

            Responde SOLO con este JSON (sin markdown, sin explicaciones):
            {
              "tipo": "TRANSFERENCIA", 
              "monto": 0.0,
              "categoria": "Otros",
              "descripcion": "Texto breve",
              "cuenta_origen": "NombreCuentaOrigen",
              "cuenta_destino": "NombreCuentaDestino"
            }
        """.trimIndent()

        val request = GeminiRequest(listOf(Content(listOf(Part(prompt)))))

        return try {
            val response = api.generarContenido(apiKey, request)
            val jsonString = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (jsonString != null) {
                // Limpieza robusta por si Gemini devuelve bloques de código ```json ... ```
                val jsonLimpio = jsonString
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

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