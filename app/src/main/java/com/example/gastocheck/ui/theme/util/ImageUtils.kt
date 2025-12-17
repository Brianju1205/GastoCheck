package com.example.gastocheck.ui.theme.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageUtils {

    // 1. Crear URI Temporal (Para que la cámara guarde la foto aquí primero)
    fun crearUriParaFoto(context: Context): Uri {
        val imagePath = File(context.cacheDir, "images")
        if (!imagePath.exists()) imagePath.mkdirs()

        // Creamos el archivo temporal
        val file = File(imagePath, "temp_camara_${System.currentTimeMillis()}.jpg")

        // Retornamos la URI con los permisos del Provider
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Asegúrate de que esto coincida con tu Manifest
            file
        )
    }

    // 2. Guardar Permanentemente (Copia la foto temporal a la carpeta privada)
    fun guardarImagenEnInterno(context: Context, uriOrigen: Uri): String? {
        return try {
            val contentResolver = context.contentResolver

            // Carpeta PERMANENTE "comprobantes" dentro de los archivos de la app
            val directorioDestino = File(context.filesDir, "comprobantes")
            if (!directorioDestino.exists()) directorioDestino.mkdirs()

            // Nombre único para que no se sobrescriban
            val nombreArchivo = "IMG_${UUID.randomUUID()}.jpg"
            val archivoDestino = File(directorioDestino, nombreArchivo)

            // Copiamos los datos (bytes) del origen al destino
            val inputStream: InputStream? = contentResolver.openInputStream(uriOrigen)
            val outputStream = FileOutputStream(archivoDestino)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // Retornamos la RUTA ABSOLUTA del archivo guardado
            archivoDestino.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}