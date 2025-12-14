/*package com.example.gastocheck.data.gemini

import android.util.Log
import com.example.gastocheck.data.database.entity.BalanceSnapshotEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.ui.theme.util.DateUtils
import com.google.gson.Gson
import okhttp3.OkHttpClient // <--- NUEVO IMPORT
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit // <--- NUEVO IMPORT
import javax.inject.Inject

class GeminiRepository @Inject constructor() {

    private val apiKey = "AIzaSyAD4bRLnnPH_9OXLlrTZ3h4tziIoRPSdTE"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // Tiempo para conectar con el servidor
        .readTimeout(60, TimeUnit.SECONDS)    // Tiempo esperando que Gemini "piense" y responda
        .writeTimeout(60, TimeUnit.SECONDS)   // Tiempo enviando los datos
        .build()
    private val api: GeminiApi = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApi::class.java)

    // -------------------------------
    // INTERPRETACIÓN DE VOZ
    // -------------------------------
    suspend fun interpretarTexto(texto: String): TransaccionInterpretada? {
        val prompt = """
            Actúa como un motor de procesamiento de datos financieros.
            Analiza la siguiente frase y extrae la información en formato JSON estricto.
            
            Frase: "$texto"
            
            Reglas de Inferencia:
            1. Si no se menciona una categoría explícita, deduce la más lógica basada en el contexto (ej: "Uber" -> "Transporte", "Cine" -> "Entretenimiento").
            2. Si no se menciona el tipo, asume que es un "GASTO" por defecto, a menos que palabras clave indiquen "INGRESO" (gané, recibí, cobré) o "TRANSFERENCIA".
            3. Responde ÚNICAMENTE con el JSON.
            
            Estructura JSON requerida:
            {
              "tipo": "GASTO | INGRESO | TRANSFERENCIA | META",
              "monto": number,
              "categoria": "string",
              "descripcion": "string",
              "cuenta_origen": "string",
              "cuenta_destino": "string"
            }
        """.trimIndent()

        return llamadaGemini(prompt, TransaccionInterpretada::class.java)
    }

    // -------------------------------
    // ANÁLISIS FINANCIERO
    // -------------------------------
    suspend fun analizarFinanzas(
        historial: List<BalanceSnapshotEntity>,
        transacciones: List<TransaccionEntity>
    ): AnalisisFinancieroResponse? {

        // Formato compacto para ahorrar tokens y acelerar la respuesta
        val resumenHistorial = historial.takeLast(30).joinToString(";") {
            "${DateUtils.formatearFechaAmigable(it.fecha)}:$${it.saldo.toInt()}"
        }

        val resumenTransacciones = transacciones.take(15).joinToString(";") {
            "${it.categoria}($${it.monto.toInt()})"
        }

        val prompt = """
            Rol: Eres un asesor financiero personal experto.
            Tono: Profesional, claro, empático y fácil de entender.
            
            Tarea: Analiza el historial de saldos y las transacciones recientes para dar un diagnóstico de salud financiera.
            
            Reglas de Estilo:
            1. NO uses jerga técnica compleja (evita términos como "flujo de caja libre", "volatilidad", "activos").
            2. Usa un lenguaje natural que cualquier adulto pueda comprender.
            3. Sé conciso y ve al grano.
            4. Responde ÚNICAMENTE con el JSON.

            Datos:
            HISTORIAL: $resumenHistorial
            TRANSACCIONES: $resumenTransacciones

            Estructura JSON:
            {
              "proyeccion_fin_mes": number (Calcula una estimación realista del saldo final),
              "tendencia_texto": "string" (Describe la dirección de las finanzas en una frase clara. Ej: "Tu saldo se mantiene estable con un ligero crecimiento"),
              "patrones": ["string"] (Lista 1 o 2 comportamientos detectados. Ej: "La mayoría de tus gastos son en comida"),
              "insight_principal": "string" (La conclusión más importante. Ej: "Estás gastando más rápido de lo que ingresas"),
              "recomendacion_accionable": "string" (Un consejo práctico y realizable. Ej: "Intenta reducir las salidas a comer esta semana")
            }
        """.trimIndent()

        return llamadaGemini(prompt, AnalisisFinancieroResponse::class.java)
    }


    private suspend fun <T> llamadaGemini(
        prompt: String,
        clase: Class<T>
    ): T? {
        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(prompt)))
            )
        )

        return try {
            val response = api.generarContenido(apiKey, request)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Log.e("GEMINI_HTTP", "HTTP ${response.code()} → $errorBody")
                return null
            }

            // 1. Obtenemos el texto crudo
            val jsonString = response.body()
                ?.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text

            Log.d("GEMINI_RAW", "Respuesta cruda: $jsonString")

            if (jsonString.isNullOrBlank()) return null

            // 2. LIMPIEZA QUIRÚRGICA
            val startIndex = jsonString.indexOf("{")
            val endIndex = jsonString.lastIndexOf("}")

            if (startIndex != -1 && endIndex != -1) {
                val jsonLimpio = jsonString.substring(startIndex, endIndex + 1)
                Log.d("GEMINI_JSON", "JSON Limpio: $jsonLimpio")
                return Gson().fromJson(jsonLimpio, clase)
            } else {
                Log.e("GEMINI_PARSE", "No encontré un JSON válido en la respuesta")
                return null
            }

        } catch (e: Exception) {
            // AQUÍ ES DONDE ATRAPABAS EL TIMEOUT
            Log.e("GEMINI_CRASH", "Error: ${e.message}")
            e.printStackTrace()
            null
        }
    }

}
*/
package com.example.gastocheck.data.gemini // Puedes mantener el paquete o cambiar a .data.groq

import android.util.Log
import com.example.gastocheck.data.database.entity.BalanceSnapshotEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.ui.theme.util.DateUtils
import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// ---------------------------------------------------------
// 1. MODELOS DE DATOS (Formato Estándar OpenAI/Groq)
// ---------------------------------------------------------
data class GroqRequest(
    val model: String = "llama-3.3-70b-versatile", // El modelo más potente y rápido de Groq
    val messages: List<GroqMessage>,
    val temperature: Double = 0.1 // Temperatura baja para respuestas precisas (JSON)
)

data class GroqMessage(
    val role: String,
    val content: String
)

data class GroqResponse(val choices: List<GroqChoice>)
data class GroqChoice(val message: GroqMessage)

// ---------------------------------------------------------
// 2. INTERFAZ API
// ---------------------------------------------------------
interface GroqApi {
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun chatCompletion(
        @retrofit2.http.Header("Authorization") token: String,
        @Body request: GroqRequest
    ): Response<GroqResponse>
}

// ---------------------------------------------------------
// 3. REPOSITORIO (LÓGICA PRINCIPAL)
// ---------------------------------------------------------
class GeminiRepository @Inject constructor() { // Mantenemos el nombre para no romper tu inyección de dependencias

    // TU CLAVE DE GROQ (Ya configurada)
    private val apiKey = ""

    // URL BASE DE GROQ (Compatible con OpenAI)
    private val baseUrl = "https://api.groq.com/openai/v1/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Groq es rapidísimo, 30s sobran
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: GroqApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GroqApi::class.java)

    // ==========================================
    // FUNCIÓN 1: INTERPRETAR VOZ
    // ==========================================
    suspend fun interpretarTexto(texto: String): TransaccionInterpretada? {
        val prompt = """
            Eres una API que convierte lenguaje natural a JSON.
            Frase: "$texto"
            
            Reglas:
            1. Si no hay categoría, deduce una lógica (Uber->Transporte).
            2. Si no hay tipo, asume GASTO.
            3. Si es transferencia detecta la primer cuenta como cuenta origen y la segunda cuenta como cuenta destino
            4. Si solo menciona una cuenta detectalo como cuenta origen
            5. Responde SOLO con el JSON.
            
            JSON Objetivo:
            {
              "tipo": "GASTO | INGRESO | TRANSFERENCIA | META",
              "monto": 0.0,
              "categoria": "string",
              "descripcion": "string",
              "cuenta_origen": "string",
              "cuenta_destino": "string"
            }
        """.trimIndent()

        return llamadaGroq(prompt, TransaccionInterpretada::class.java)
    }

    // ==========================================
    // FUNCIÓN 2: ANÁLISIS FINANCIERO (Coach)
    // ==========================================
    suspend fun analizarFinanzas(
        historial: List<BalanceSnapshotEntity>,
        transacciones: List<TransaccionEntity>
    ): AnalisisFinancieroResponse? {

        val resumenHistorial = historial.takeLast(30).joinToString(";") {
            "${DateUtils.formatearFechaAmigable(it.fecha)}:$${it.saldo.toInt()}"
        }

        val resumenTransacciones = transacciones.take(15).joinToString(";") {
            "${it.categoria}($${it.monto.toInt()})"
        }

        val prompt = """
            Rol: Eres un asesor financiero personal experto.
            Tono: Profesional, claro, empático y fácil de entender.
            
            Tarea: Analiza el historial de saldos y las transacciones recientes para dar un diagnóstico de salud financiera.
            
            Reglas de Estilo:
            1. NO uses jerga técnica compleja (evita términos como "flujo de caja libre", "volatilidad", "activos").
            2. Usa un lenguaje natural que cualquier adulto pueda comprender.
            3. Sé conciso y ve al grano.
            4. Responde ÚNICAMENTE con el JSON.

            Datos:
            HISTORIAL: $resumenHistorial
            TRANSACCIONES: $resumenTransacciones

            Estructura JSON:
            {
              "proyeccion_fin_mes": number (Calcula una estimación realista del saldo final),
              "tendencia_texto": "string" (Describe la dirección de las finanzas en una frase clara. Ej: "Tu saldo se mantiene estable con un ligero crecimiento"),
              "patrones": ["string"] (Lista 1 o 2 comportamientos detectados. Ej: "La mayoría de tus gastos son en comida"),
              "insight_principal": "string" (La conclusión más importante. Ej: "Estás gastando más rápido de lo que ingresas"),
              "recomendacion_accionable": "string" (Un consejo práctico y realizable. Ej: "Intenta reducir las salidas a comer esta semana")
            }
        """.trimIndent()

        return llamadaGroq(prompt, AnalisisFinancieroResponse::class.java)
    }

    // ==========================================
    // FUNCIÓN CENTRAL
    // ==========================================
    private suspend fun <T> llamadaGroq(prompt: String, clase: Class<T>): T? {
        // Preparamos el mensaje para Llama 3
        val messages = listOf(
            GroqMessage(role = "system", content = "Eres un asistente financiero experto que SIEMPRE responde en formato JSON puro, sin markdown ni explicaciones."),
            GroqMessage(role = "user", content = prompt)
        )

        val request = GroqRequest(messages = messages)

        return try {
            val response = api.chatCompletion(apiKey, request)

            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string()
                Log.e("GROQ_HTTP", "Error ${response.code()}: $errorMsg")
                return null
            }

            // Obtenemos el contenido
            val content = response.body()?.choices?.firstOrNull()?.message?.content

            Log.d("GROQ_RAW", "Respuesta: $content")

            if (content.isNullOrBlank()) return null

            // Limpieza de JSON (Llama a veces pone ```json al inicio)
            val startIndex = content.indexOf("{")
            val endIndex = content.lastIndexOf("}")

            if (startIndex != -1 && endIndex != -1) {
                val jsonLimpio = content.substring(startIndex, endIndex + 1)
                Gson().fromJson(jsonLimpio, clase)
            } else {
                Log.e("GROQ_PARSE", "No se encontró JSON válido")
                null
            }

        } catch (e: Exception) {
            Log.e("GROQ_CRASH", "Error crítico: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}