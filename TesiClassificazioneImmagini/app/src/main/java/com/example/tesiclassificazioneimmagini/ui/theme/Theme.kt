package com.example.tesiclassificazioneimmagini.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// Definisci i colori personalizzati
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val Bordeaux = Color(0xFF800020)

// Definisci il nuovo schema di colori
private val CustomColorScheme = lightColorScheme(
    primary = Bordeaux,        // Colore principale (bottoni, elementi interattivi)
    onPrimary = White,          // Colore del testo sul primary
    surface = White,            // Colore di sfondo (card, superfici)
    background = White,         // Colore di sfondo principale
    onBackground = Black,       // Colore del testo sul background
    onSurface = Black,          // Colore del testo sulle superfici
    secondary = Black,          // Colore secondario
    onSecondary = White,        // Colore del testo sul secondario
    tertiary = Bordeaux,        // Terziario, usato per accenti
    onTertiary = White          // Colore del testo sul terziario
)

@Composable
fun TesiClassificazioneImmaginiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Se vuoi usare colori dinamici su Android 12+
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Se vuoi usare il tema scuro di default
        darkTheme -> DarkColorScheme
        // Altrimenti, usa il tema chiaro personalizzato
        else -> CustomColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
