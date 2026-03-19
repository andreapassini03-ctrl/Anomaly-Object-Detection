package com.example.tesiclassificazioneimmagini.viewmodel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesiclassificazioneimmagini.model.TfLiteAnomalyDetector
import com.example.tesiclassificazioneimmagini.model.AnomalyResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "AnomalyViewModel"

// ViewModel per gestire lo stato dell'applicazione
class AnomalyViewModel : ViewModel() {
    var selectedBitmap by mutableStateOf<Bitmap?>(null)
    var isAnomaly by mutableStateOf<Boolean?>(null)
    var mseValue by mutableFloatStateOf(0f)
    var anomalyThreshold by mutableFloatStateOf(0.001872f) // Soglia di anomalia di esempio
    var lastError by mutableStateOf<String?>(null)

    // Nuove bitmap per visualizzazione
    var heatmapBitmap by mutableStateOf<Bitmap?>(null)
    var reconstructedBitmap by mutableStateOf<Bitmap?>(null)

    // Parametri avanzati per la generazione della heatmap
    var heatmapPower by mutableFloatStateOf(0.5f)
    var heatmapSmoothing by mutableStateOf(true)
    var heatmapLowPercentile by mutableFloatStateOf(0.05f)
    var heatmapHighPercentile by mutableFloatStateOf(0.995f)

    private var anomalyDetector: TfLiteAnomalyDetector? = null

    fun loadModel(context: Context) {
        Log.d(TAG, "Tentativo di caricare il modello TFLite.")
        try {
            anomalyDetector = TfLiteAnomalyDetector(context, "anomaly_detector_improved.tflite")
            lastError = null
            Log.i(TAG, "Modello TFLite caricato con successo.")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel caricamento del modello TFLite: ${e.message}")
            e.printStackTrace()
            lastError = e.message ?: "Errore nel caricamento del modello"
        }
    }

    fun clearVisuals() {
        heatmapBitmap = null
        reconstructedBitmap = null
        isAnomaly = null
        mseValue = 0f
    }

    fun runInference(onFinished: (Boolean?) -> Unit) {
        viewModelScope.launch {
            Log.d(TAG, "Avviata l'inferenza nel ViewModel.")
            if (selectedBitmap == null) {
                lastError = "Bitmap nullo"
                onFinished(null)
                return@launch
            }
            if (anomalyDetector == null) {
                lastError = "Modello non caricato"
                onFinished(null)
                return@launch
            }
            isAnomaly = null
            mseValue = 0f
            lastError = null
            heatmapBitmap = null
            reconstructedBitmap = null

            val result: AnomalyResult? = withContext(Dispatchers.Default) {
                Log.d(TAG, "Esecuzione dell'inferenza dettagliata in background.")
                try {
                    anomalyDetector!!.detectDetailed(
                        bitmap = selectedBitmap!!,
                        power = heatmapPower,
                        smooth = heatmapSmoothing,
                        lowPercentile = heatmapLowPercentile,
                        highPercentile = heatmapHighPercentile
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Errore durante l'esecuzione dell'inferenza: ${e.message}")
                    e.printStackTrace()
                    lastError = e.message ?: "Eccezione durante l'inferenza"
                    null
                }
            }

            if (result == null) {
                Log.e(TAG, "Risultato dell'inferenza è nullo. Ritorno 'null'.")
                onFinished(null)
                return@launch
            }

            mseValue = result.mse
            heatmapBitmap = result.heatmapOverlay
            reconstructedBitmap = result.reconstructedBitmap
            isAnomaly = result.mse > anomalyThreshold
            Log.i(TAG, "Inferenza completata. MSE: $mseValue. Soglia: $anomalyThreshold. Rilevata anomalia: $isAnomaly")
            onFinished(isAnomaly)
        }
    }

    fun saveHeatmap(context: Context) {
        val bmp = heatmapBitmap ?: return
        try {
            val fileName = "heatmap_result_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.TITLE, fileName)
                put(MediaStore.Images.Media.DESCRIPTION, "Heatmap Anomalia")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            }
            Log.i(TAG, "Heatmap salvata: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Errore salvataggio heatmap", e)
        }
    }
}
