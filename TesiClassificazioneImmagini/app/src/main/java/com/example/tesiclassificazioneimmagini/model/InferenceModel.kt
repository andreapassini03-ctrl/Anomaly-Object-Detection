package com.example.tesiclassificazioneimmagini.model

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.example.tesiclassificazioneimmagini.yolo_v5.COCOLabels
import com.example.tesiclassificazioneimmagini.yolo_v5.Detection
import com.example.tesiclassificazioneimmagini.yolo_v5.YoloV5Classifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class InferenceModel {

    suspend fun detect(
        selectedBitmap: Bitmap?,
        detectionThreshold: Float,
        classifier: YoloV5Classifier
    ): Pair<Bitmap, List<Detection>>? {
        return withContext(Dispatchers.Default) {
            try {
                if (selectedBitmap == null) return@withContext null
                val output = classifier.detect(selectedBitmap)
                drawBoxes(
                    selectedBitmap.copy(Bitmap.Config.ARGB_8888, true),
                    output,
                    detectionThreshold
                )
            } catch (e: Exception) {
                Log.e("YOLO", "Errore durante l'inferenza", e)
                null
            }
        }
    }

    fun saveImage(context: Context, bitmap: Bitmap) {
        try {
            val fileName = "detection_result_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.TITLE, fileName)
                put(MediaStore.Images.Media.DESCRIPTION, "fileName_Yolov5")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    Toast.makeText(context, "Immagine salvata", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("SaveImage", "Errore nel salvataggio dell'immagine", e)
            Toast.makeText(context, "Errore nel salvataggio dell'immagine", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawBoxes(
        bitmap: Bitmap,
        outputs: Array<Array<FloatArray>>,
        threshold: Float
    ): Pair<Bitmap, List<Detection>> {
        val canvas = Canvas(bitmap)

        val boxPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 60f
            style = Paint.Style.FILL
        }

        val detections = mutableListOf<Detection>()

        for (i in outputs[0].indices) {
            val prediction = outputs[0][i]
            val x = prediction[0]
            val y = prediction[1]
            val w = prediction[2]
            val h = prediction[3]
            val objectness = prediction[4]

            if (objectness < threshold) continue

            var classId = -1
            var maxProb = 0f
            for (c in 5 until prediction.size) {
                if (prediction[c] > maxProb) {
                    maxProb = prediction[c]
                    classId = c - 5
                }
            }

            val left = x - w / 2
            val top = y - h / 2
            val right = x + w / 2
            val bottom = y + h / 2

            detections.add(Detection(left, top, right, bottom, objectness, classId))
        }

        val filtered = Detection.applyNMS(detections, 0.7f)

        for (detection in filtered) {
            val left = detection.left * bitmap.width
            val top = detection.top * bitmap.height
            val right = detection.right * bitmap.width
            val bottom = detection.bottom * bitmap.height

            canvas.drawRect(left, top, right, bottom, boxPaint)

            val className = COCOLabels.getLabel(detection.classId)
            val confidence = String.format(Locale.US, "%.2f", detection.objectness)
            canvas.drawText("$className ($confidence)", left, top + 60f, textPaint)

        }

        return Pair(bitmap, filtered)
    }
}
