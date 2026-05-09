package com.example.Gym_App.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorage {
    private const val FOLDER_NAME = "rutinas_images"

    fun saveImage(context: Context, uri: Uri): String? {
        val folder = File(context.filesDir, FOLDER_NAME)
        if (!folder.exists()) folder.mkdirs()

        val fileName = "${UUID.randomUUID()}.jpg"
        val file = File(folder, fileName)

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun getAllImages(context: Context): List<File> {
        val folder = File(context.filesDir, FOLDER_NAME)
        if (!folder.exists()) return emptyList()
        return folder.listFiles { file -> file.extension.lowercase() in listOf("jpg", "jpeg", "png") }?.toList() ?: emptyList()
    }
}
