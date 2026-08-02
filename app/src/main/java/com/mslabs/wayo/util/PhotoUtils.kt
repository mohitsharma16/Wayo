package com.mslabs.wayo.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object PhotoUtils {

    fun createImageFile(context: Context): File {
        val dir = File(context.filesDir, "parking_photos").apply { mkdirs() }
        return File(dir, "spot_${System.currentTimeMillis()}.jpg")
    }

    fun uriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
