package com.example.tesiclassificazioneimmagini.yolo_v5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
Tengo solo quello con la confidenza più alta e scarto l’altro.
 */

public class Detection {
    private final float left, top, right, bottom, objectness;
    private final int classId;

    public Detection(float left, float top, float right, float bottom, float objectness, int classId) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.objectness = objectness;
        this.classId = classId;
    }

    // Getter pubblici per accesso cross-package (Kotlin li esporrà come proprietà)
    public float getLeft() { return left; }
    public float getTop() { return top; }
    public float getRight() { return right; }
    public float getBottom() { return bottom; }
    public float getObjectness() { return objectness; }
    public int getClassId() { return classId; }

    // Funzione per applicare il Non-Maximum Suppression (NMS)
    public static List<Detection> applyNMS(List<Detection> detections, float iouThreshold) {
        // Ordinare per confidenza
        Collections.sort(detections, (d1, d2) -> Float.compare(d2.objectness, d1.objectness));

        List<Detection> filteredDetections = new ArrayList<>();

        while (!detections.isEmpty()) {
            // Prendi la previsione con la confidenza più alta
            Detection bestDetection = detections.remove(0);
            filteredDetections.add(bestDetection);

            // Filtra le detections che si sovrappongono troppo (IoU > soglia)
            detections.removeIf(detection -> computeIoU(bestDetection, detection) > iouThreshold);
        }

        return filteredDetections;
    }

    // Funzione per calcolare l'Intersection over Union (IoU)
    private static float computeIoU(Detection d1, Detection d2) {
        float left = Math.max(d1.left, d2.left);
        float top = Math.max(d1.top, d2.top);
        float right = Math.min(d1.right, d2.right);
        float bottom = Math.min(d1.bottom, d2.bottom);

        if (right < left || bottom < top) {
            return 0f;  // Non c'è sovrapposizione
        }

        float intersectionArea = (right - left) * (bottom - top);
        float d1Area = (d1.right - d1.left) * (d1.bottom - d1.top);
        float d2Area = (d2.right - d2.left) * (d2.bottom - d2.top);

        float unionArea = d1Area + d2Area - intersectionArea;
        return intersectionArea / unionArea;  // IoU
    }

}
