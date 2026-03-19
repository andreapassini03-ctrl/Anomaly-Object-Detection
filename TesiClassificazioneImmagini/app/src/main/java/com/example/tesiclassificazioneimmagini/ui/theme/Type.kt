package com.example.tesiclassificazioneimmagini.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.tesiclassificazioneimmagini.R

// Font personalizzato (Comic Sans o qualsiasi altro font che hai nominato comici.ttf)
val ComiciFontFamily = FontFamily(
    Font(R.font.comici)
)

// Tipografia globale con il font personalizzato
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = ComiciFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = -0.25.sp
    ),
    displayMedium = TextStyle(
        fontFamily = ComiciFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 52.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ComiciFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ComiciFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ComiciFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )

)
