package com.example.tesiclassificazioneimmagini.viewmodel

import android.graphics.Bitmap
import android.content.res.AssetManager
import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesiclassificazioneimmagini.model.InferenceModel
import com.example.tesiclassificazioneimmagini.yolo_v5.YoloV5Classifier
import com.example.tesiclassificazioneimmagini.yolo_v5.Detection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ViewModel
class YoloViewModel : ViewModel() {
    var selectedBitmap by mutableStateOf<Bitmap?>(null)
    var checkInference by mutableStateOf(false)
    // Aggiungi la variabile di stato per le rilevazioni
    var detectedObjects by mutableStateOf<List<Detection>>(emptyList())

    private var classifier: YoloV5Classifier? = null
    var threshold by mutableFloatStateOf(0.55f)
    val inferenceModel: InferenceModel = InferenceModel()

    fun loadModel(assetManager: AssetManager) {
        try {
            classifier = YoloV5Classifier(assetManager, "yolov5s-fp16.tflite")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun runInference(onFinished: (Bitmap?) -> Unit) {
        viewModelScope.launch {
            checkInference = false
            val result: Pair<Bitmap, List<Detection>>? = withContext(Dispatchers.Default) {
                // Esegui l'inferenza e ottieni il bitmap annotato e la lista di rilevazioni
                inferenceModel.detect(selectedBitmap, threshold, classifier!!)
            }
            checkInference = true
            if (result != null) {
                val detections = result.second
                val bitmap = result.first
                detectedObjects = detections
                // Aggiorna l'immagine selezionata con quella annotata (box visibili)
                selectedBitmap = bitmap
                onFinished(bitmap)
            } else {
                detectedObjects = emptyList()
                onFinished(null)
            }
        }
    }

    fun saveImage(context: Context, bitmap: Bitmap) {
        inferenceModel.saveImage(context, bitmap)
    }
}
