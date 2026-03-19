package com.example.tesiclassificazioneimmagini.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.TransformToGrayscaleOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.min

private const val TAG = "TfLiteAnomalyDetector"

// Risultato dettagliato dell'inferenza per visualizzare heatmap
data class AnomalyResult(
    val mse: Float,
    val heatmapOverlay: Bitmap?,
    val reconstructedBitmap: Bitmap? = null
)

// Classe per l'interprete TFLite e la logica di inferenza
class TfLiteAnomalyDetector(context: Context, modelPath: String) {

    private val interpreter: Interpreter
    private val inputShape: IntArray
    private val outputShape: IntArray
    private val inputType: DataType
    private val outputType: DataType

    // Processore immagine creato in base ai canali del modello
    private val imageProcessor: ImageProcessor
    private val inputTensor: TensorImage

    init {
        Log.d(TAG, "Inizializzazione del rilevatore di anomalie.")
        // Carica il modello TFLite
        val modelBuffer: ByteBuffer = FileUtil.loadMappedFile(context, modelPath)
        val options = Interpreter.Options()
        interpreter = Interpreter(modelBuffer, options)
        Log.i(TAG, "Interprete TFLite caricato.")

        // Ottieni i dettagli del modello
        inputShape = interpreter.getInputTensor(0).shape()
        outputShape = interpreter.getOutputTensor(0).shape()
        inputType = interpreter.getInputTensor(0).dataType()
        outputType = interpreter.getOutputTensor(0).dataType()
        Log.d(TAG, "Forma input: ${inputShape.joinToString()}, Forma output: ${outputShape.joinToString()}")
        Log.d(TAG, "Tipo input: $inputType, Tipo output: $outputType")

        // Pre-processore per l'immagine
        val channels = inputShape.lastOrNull() ?: 1 // Prende l'ultimo elemento della forma
        val imageSizeX = inputShape[1]
        val imageSizeY = inputShape[2]

        val imageProcessorBuilder = ImageProcessor.Builder()
            .add(ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.BILINEAR))

        // Aggiunge la conversione in scala di grigi se il modello si aspetta 1 canale
        if (channels == 1) {
            imageProcessorBuilder.add(TransformToGrayscaleOp())
        }

        imageProcessorBuilder.add(NormalizeOp(0.0f, 255.0f))

        imageProcessor = imageProcessorBuilder.build()
        Log.d(TAG, "ImageProcessor configurato. Canali: $channels, Dimensione: ${imageSizeX}x${imageSizeY}.")

        // Inizializza il tensore di input con il tipo corretto
        inputTensor = TensorImage(inputType)
        Log.d(TAG, "TensorImage di input inizializzato.")
    }

    // Esegue l'inferenza e restituisce il MSE tra input pre-processato e output (compatibilità)
    fun detect(bitmap: Bitmap): Float {
        return detectDetailed(bitmap).mse
    }

    // Nuova funzione: ritorna anche heatmap e ricostruzione
    fun detectDetailed(
        bitmap: Bitmap,
        overlayAlpha: Int = 140,
        smooth: Boolean = true,
        lowPercentile: Float = 0.05f,
        highPercentile: Float = 0.995f,
        power: Float = 0.5f // Nuovo: per contrasto (0.5 = radice quadrata)
    ): AnomalyResult {
        Log.d(TAG, "Avviato detectDetailed per generare heatmap.")
        // Carica e pre-processa l'immagine
        inputTensor.load(bitmap)
        val processedImage = imageProcessor.process(inputTensor)

        val b = outputShape.getOrNull(0) ?: 1
        val h = outputShape.getOrNull(1) ?: 1
        val w = outputShape.getOrNull(2) ?: 1
        val c = outputShape.getOrNull(3) ?: 1
        val output4d = Array(b) { Array(h) { Array(w) { FloatArray(c) } } }

        val inputByteBuffer = processedImage.buffer
        inputByteBuffer.order(ByteOrder.nativeOrder())
        inputByteBuffer.rewind()

        interpreter.run(inputByteBuffer, output4d)

        val inFloats = processedImage.tensorBuffer.floatArray

        // Flatten output
        val outFloats = FloatArray(b * h * w * c)
        var idx = 0
        for (bi in 0 until b) {
            for (yi in 0 until h) {
                for (xi in 0 until w) {
                    val row = output4d[bi][yi][xi]
                    for (ci in 0 until c) {
                        outFloats[idx++] = row[ci]
                    }
                }
            }
        }

        val n = min(inFloats.size, outFloats.size)
        if (n == 0) return AnomalyResult(0f, null, null)

        // Calcolo MSE totale + errore per pixel
        var mseAcc = 0.0
        val pixels = w * h
        val perPixelError = FloatArray(pixels) { 0f }
        val channels = c

        // Ogni pixel ha 'channels' valori consecutivi (assumendo layout HWC normalizzato flattened)
        for (pi in 0 until pixels) {
            var acc = 0.0
            for (ci in 0 until c) {
                val base = pi * c + ci
                if (base >= n) break
                val diff = (inFloats[base] - outFloats[base]).toDouble()
                acc += diff * diff
            }
            val meanSq = (acc / c).toFloat()
            // Usa Math.pow per evitare unresolved reference
            perPixelError[pi] = if (power != 1f) Math.pow(meanSq.toDouble(), power.toDouble()).toFloat() else meanSq
            mseAcc += acc
        }
        val mse = (mseAcc / n).toFloat()

        // Smoothing opzionale per ridurre rumore speckle
        val processedErrors = if (smooth) smooth3x3(perPixelError, w, h) else perPixelError

        // Normalizzazione robusta via percentili per evitare "tutto rosso"
        val (pLow, pHigh) = computePercentileRange(processedErrors, lowPercentile, highPercentile)
        val span = if (pHigh > pLow) (pHigh - pLow) else 1e-6f
        for (i in processedErrors.indices) {
            val v = (processedErrors[i] - pLow) / span
            processedErrors[i] = v.coerceIn(0f, 1f)
        }

        val heatmap = generateHeatmapBitmap(processedErrors, w, h)
        val reconstructedBitmap = rebuildBitmap(outFloats, w, h, c)

        // Scala heatmap alle dimensioni originali e crea overlay
        val scaledHeatmap = if (bitmap.width != w || bitmap.height != h) {
            Bitmap.createScaledBitmap(heatmap, bitmap.width, bitmap.height, true)
        } else heatmap

        val overlay = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(overlay)
        val paint = Paint().apply { alpha = overlayAlpha }
        canvas.drawBitmap(scaledHeatmap, 0f, 0f, paint)

        Log.d(TAG, "Heatmap generata. MSE: $mse")
        return AnomalyResult(mse, overlay, reconstructedBitmap)
    }

    private fun smooth3x3(src: FloatArray, w: Int, h: Int): FloatArray {
        val dst = FloatArray(src.size)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var acc = 0f
                var count = 0
                for (dy in -1..1) for (dx in -1..1) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until w && ny in 0 until h) {
                        acc += src[ny * w + nx]
                        count++
                    }
                }
                dst[y * w + x] = acc / count
            }
        }
        return dst
    }

    private fun computePercentileRange(data: FloatArray, low: Float, high: Float): Pair<Float, Float> {
        if (data.isEmpty()) return 0f to 1f
        val copy = data.copyOf()
        copy.sort()
        fun idx(p: Float): Int = (p.coerceIn(0f, 1f) * (copy.size - 1)).toInt()
        val lo = copy[idx(low)]
        val hi = copy[idx(high)]
        return lo to hi
    }

    private fun generateHeatmapBitmap(normErrors: FloatArray, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        var i = 0
        while (i < normErrors.size) {
            val v = normErrors[i].coerceIn(0f, 1f)
            val color = jetColor(v)
            val y = i / w
            val x = i - y * w
            bmp.setPixel(x, y, color)
            i++
        }
        return bmp
    }

    // Colormap Jet accurata usando formula continua
    private fun jetColor(vIn: Float): Int {
        val v = vIn.coerceIn(0f, 1f)
        fun ch(c: Float): Int = (255f * c.coerceIn(0f, 1f)).toInt()
        val r = (1.5f - abs(4f * v - 3f))
        val g = (1.5f - abs(4f * v - 2f))
        val b = (1.5f - abs(4f * v - 1f))
        return Color.argb(255, ch(r), ch(g), ch(b))
    }

    // Ricostruisce un Bitmap dall'output (solo per debug/estensioni future)
    private fun rebuildBitmap(out: FloatArray, w: Int, h: Int, c: Int): Bitmap? {
        return try {
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            if (c == 1) {
                var i = 0
                for (y in 0 until h) for (x in 0 until w) {
                    val v = (out[i++].coerceIn(0f, 1f) * 255).toInt()
                    bmp.setPixel(x, y, Color.argb(255, v, v, v))
                }
            } else {
                var idx = 0
                for (y in 0 until h) for (x in 0 until w) {
                    val r = (out[idx++].coerceIn(0f, 1f) * 255).toInt()
                    val g = (out[idx++].coerceIn(0f, 1f) * 255).toInt()
                    val b = (out[idx++].coerceIn(0f, 1f) * 255).toInt()
                    bmp.setPixel(x, y, Color.argb(255, r, g, b))
                }
            }
            bmp
        } catch (e: Exception) {
            Log.e(TAG, "Errore ricostruzione bitmap", e)
            null
        }
    }
}
