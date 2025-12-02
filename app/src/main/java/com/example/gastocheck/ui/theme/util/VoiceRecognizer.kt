package com.example.gastocheck.ui.theme.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceRecognizer(private val context: Context) {

    fun escuchar(): Flow<String> = callbackFlow {
        // Importante: SpeechRecognizer debe crearse en el hilo principal
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        // Configuración del Intent para soportar Offline
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

            // --- CLAVE PARA OFFLINE ---
            // Intentamos preferir offline si no hay red, aunque Android decide al final
            // Si quieres forzarlo siempre (puede ser menos preciso): putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { Log.d("VoiceRecognizer", "Listo") }
            override fun onBeginningOfSpeech() { Log.d("VoiceRecognizer", "Hablando...") }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { Log.d("VoiceRecognizer", "Fin habla") }

            override fun onError(error: Int) {
                // Convertimos códigos de error a mensajes legibles para depuración
                val mensaje = when(error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No se entendió nada"
                    SpeechRecognizer.ERROR_NETWORK -> "Error de red (¿Sin conexión?)"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout de red"
                    SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
                    else -> "Error genérico $error"
                }
                Log.e("VoiceRecognizer", mensaje)

                // En caso de error, enviamos "Error" para que la UI se cierre o muestre feedback
                trySend("Error")
                close()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()

                Log.d("VoiceRecognizer", "Texto reconocido: $text")

                if (!text.isNullOrEmpty()) {
                    trySend(text)
                } else {
                    trySend("Error")
                }
                close()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        // Lanzamiento seguro en Main Thread
        launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                speechRecognizer.setRecognitionListener(listener)
                speechRecognizer.startListening(intent)
            } catch (e: Exception) {
                Log.e("VoiceRecognizer", "Excepción al iniciar: ${e.message}")
                trySend("Error")
                close()
            }
        }

        awaitClose {
            launch(kotlinx.coroutines.Dispatchers.Main) {
                try {
                    speechRecognizer.stopListening()
                    speechRecognizer.destroy()
                } catch (e: Exception) { }
            }
        }
    }
}