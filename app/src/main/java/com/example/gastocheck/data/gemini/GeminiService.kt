package com.example.gastocheck.data.gemini

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

// La interfaz que define cómo hablar con Google
interface GeminiApi {
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generarContenido(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
