package com.example.gastocheck.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext // <--- IMPORTANTE
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class OcrService @Inject constructor(
    @ApplicationContext private val context: Context // <--- AGREGA ESTA ANOTACIÓN
) {
    suspend fun procesarImagen(uri: Uri): String {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}