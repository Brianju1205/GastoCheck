package com.example.gastocheck.ui.theme.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ImageUtils {
    fun crearUriParaFoto(context: Context): Uri {
        val directory = File(context.externalCacheDir, "images")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File.createTempFile(
            "IMG_${System.currentTimeMillis()}_",
            ".jpg",
            directory
        )

        // IMPORTANTE: Esto debe coincidir con el 'authorities' de tu AndroidManifest.xml
        // Tu manifest dice: android:authorities="${applicationId}.provider"
        // Por lo tanto, aquí usamos context.packageName + ".provider"
        val authority = "${context.packageName}.provider"

        return FileProvider.getUriForFile(context, authority, file)
    }
}