package com.mslabs.wayo.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun PhotoThumbnail(path: String, size: androidx.compose.ui.unit.Dp = 120.dp) {
    val bitmap = remember(path) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = "Parking spot photo",
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    }
}
