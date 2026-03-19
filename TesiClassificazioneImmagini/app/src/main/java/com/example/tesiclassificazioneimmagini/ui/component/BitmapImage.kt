package com.example.tesiclassificazioneimmagini.ui.component

import android.graphics.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color as ComposeColor

@Composable
fun BitmapImage(bitmap: Bitmap?) {
    if (bitmap == null) {
        Box(
            modifier = Modifier
                .size(350.dp)
                .background(ComposeColor.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text("Nessuna immagine", color = ComposeColor.DarkGray)
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(ComposeColor.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}