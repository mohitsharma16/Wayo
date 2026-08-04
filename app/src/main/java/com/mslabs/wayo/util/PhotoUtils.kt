package com.mslabs.wayo.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException

object PhotoUtils {

    fun createImageFile(context: Context): File {
        val dir = File(context.filesDir, "parking_photos").apply { mkdirs() }
        return File(dir, "spot_${System.currentTimeMillis()}.jpg")
    }

    fun uriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Camera capture via TakePicture()/FileProvider writes the sensor's raw
     * pixel data plus an EXIF orientation tag describing how to rotate it
     * for display -- it does NOT bake the rotation into the pixels
     * themselves. Plain BitmapFactory.decodeFile() ignores that tag
     * entirely, so photos came out rotated (90/180/270, device- and
     * orientation-dependent) everywhere they were shown. This reads the tag
     * and applies the correction once, at decode time.
     */
    fun decodeRotatedBitmap(path: String): Bitmap? {
        val bitmap = BitmapFactory.decodeFile(path) ?: return null

        val orientation = try {
            ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (e: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotationDegrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(rotationDegrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
