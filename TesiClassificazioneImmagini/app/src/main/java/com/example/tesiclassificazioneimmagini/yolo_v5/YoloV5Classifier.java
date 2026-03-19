package com.example.tesiclassificazioneimmagini.yolo_v5;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.gpu.CompatibilityList;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class YoloV5Classifier {

    private static final String TAG = "YoloV5Classifier";

    private final Interpreter tflite;
    private static final int INPUT_SIZE = 640;
    private static final int OUTPUT_ROWS = 25200;    // 640x640 image produces 25200 detections
    private static final int OUTPUT_COLUMNS = 85;    // 4 box coords + 1 obj conf + 80 class confs

    public YoloV5Classifier(AssetManager assetManager, String modelPath) throws IOException {
        try {

            // Configura le opzioni dell’interprete
            Interpreter.Options options = new Interpreter.Options();

            // Aggiungi il delegato GPU
            GpuDelegate gpuDelegate = new GpuDelegate();
            options.addDelegate(gpuDelegate);

            // Crea l’interprete TensorFlow Lite e assegna il delegato GPU
            tflite = new Interpreter(loadModelFile(assetManager, modelPath), options);


            Log.d(TAG, "Modello caricato con successo");
        } catch (Exception e) {
            Log.e(TAG, "Errore durante il caricamento del modello: " + e.getMessage());
            throw e;
        }
    }

    // Carica il file .tflite dal file system
    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Esegue l'inferenza sul bitmap passato
    public float[][][] detect(Bitmap bitmap) {
        // Ridimensiona l'immagine a 640x640
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);

        // Converte il bitmap in input ByteBuffer (formato float32 RGB normalizzato [0,1])
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

        // Alloca array di output della dimensione corretta per YOLOv5
        float[][][] output = new float[1][OUTPUT_ROWS][OUTPUT_COLUMNS];

        // Esegue inferenza
        tflite.run(inputBuffer, output);

        Log.d(TAG, "Output: [1][" + output[0].length + "][" + output[0][0].length + "]");
        Log.d(TAG, "Esempio valore output[0][0][0]: " + output[0][0][0]);
        return output;
    }

    /*
    // Converte Bitmap in ByteBuffer con valori int8 normalizzati
    // Nota: Qui assumiamo che il modello usi uint8 quantization (0-255). Se il modello usa signed int8
    // (-128 a 127), devi adattare la limitazione e il tipo.
    
    private ByteBuffer convertBitmapToByteBuffer_INT8(Bitmap bitmap) {
    // Parametri di quantizzazione del modello (modifica questi con i tuoi valori esatti)
    float scale = 0.0039216f;  // esempio comune: 1/255
    int zeroPoint = 128;       // esempio comune

    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3);
    byteBuffer.order(ByteOrder.nativeOrder());

    int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
    bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

    for (int pixel : pixels) {
        // Ottieni i valori RGB normalizzati in [0,1]
        float r = ((pixel >> 16) & 0xFF) / 255.0f;
        float g = ((pixel >> 8) & 0xFF) / 255.0f;
        float b = (pixel & 0xFF) / 255.0f;

        // Quantizza ogni canale
        int rQuant = Math.round(r / scale) + zeroPoint;
        int gQuant = Math.round(g / scale) + zeroPoint;
        int bQuant = Math.round(b / scale) + zeroPoint;

        // Limita i valori nell’intervallo [0, 255]
        rQuant = Math.min(255, Math.max(0, rQuant));
        gQuant = Math.min(255, Math.max(0, gQuant));
        bQuant = Math.min(255, Math.max(0, bQuant));

        // Inserisci nel ByteBuffer come byte (signed)
        byteBuffer.put((byte)(rQuant & 0xFF));
        byteBuffer.put((byte)(gQuant & 0xFF));
        byteBuffer.put((byte)(bQuant & 0xFF));
    }

    return byteBuffer;
}

     */

    // Converte Bitmap in ByteBuffer con valori float32 normalizzati
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        for (int pixel : pixels) {
            float r = ((pixel >> 16) & 0xFF) / 255.0f;
            float g = ((pixel >> 8) & 0xFF) / 255.0f;
            float b = (pixel & 0xFF) / 255.0f;

            byteBuffer.putFloat(r);
            byteBuffer.putFloat(g);
            byteBuffer.putFloat(b);
        }

        return byteBuffer;
    }
}
