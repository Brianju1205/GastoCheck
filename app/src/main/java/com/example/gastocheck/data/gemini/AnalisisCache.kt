package com.example.gastocheck.data.gemini

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalisisCache @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("finanzas_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Guarda el análisis y la "huella" de los datos actuales
    fun guardarAnalisis(firma: String, analisis: AnalisisFinancieroResponse) {
        prefs.edit().apply {
            putString("last_signature", firma)
            putString("last_analysis_json", gson.toJson(analisis))
            apply()
        }
    }

    // Intenta recuperar el análisis si la huella coincide
    fun obtenerAnalisisCacheado(firmaActual: String): AnalisisFinancieroResponse? {
        val firmaGuardada = prefs.getString("last_signature", "")
        val jsonGuardado = prefs.getString("last_analysis_json", "")

        if (firmaGuardada == firmaActual && !jsonGuardado.isNullOrEmpty()) {
            return try {
                gson.fromJson(jsonGuardado, AnalisisFinancieroResponse::class.java)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    // Borra la caché (útil si el usuario quiere forzar actualización)
    fun limpiarCache() {
        prefs.edit().clear().apply()
    }
}