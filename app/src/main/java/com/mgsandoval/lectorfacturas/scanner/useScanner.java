package com.mgsandoval.lectorfacturas.scanner;

import static com.mgsandoval.lectorfacturas.MainActivity.TAG;
import android.util.Log;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;
import com.google.mlkit.vision.common.InputImage;
import com.mgsandoval.lectorfacturas.MainActivity;

public class useScanner {

    private final MainActivity activity;
    public useScanner(MainActivity activity) {
        this.activity = activity;
    }

    public void toggleAnalysis() {
        if (activity.camera == null) return;

        // Toggle the state inside the camera helper
        activity.camera.isAnalysisRunning = !activity.camera.isAnalysisRunning;

        if (activity.camera.isAnalysisRunning) {
            activity.btnAnalizar.setImageResource(android.R.drawable.ic_media_pause);
            activity.tvResultados.setText("Analizando en vivo...");
        } else {
            activity.btnAnalizar.setImageResource(android.R.drawable.ic_media_play);
            activity.tvResultados.setText("Análisis detenido");
        }

        // Restart camera to apply the new use cases (with or without analysis)
        activity.camera.startCamera();
    }

    @ExperimentalGetImage
    public void processImageProxy(ImageProxy imageProxy) {
        if (!activity.isProcessing.compareAndSet(false, true)) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            activity.textRecognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String text = visionText.getText();
                        if (!text.isEmpty()) {
                            String total = extractTotal(text);
                            if (!total.equals("No encontrado")) {
                                activity.runOnUiThread(() -> {
                                    activity.tvResultados.setText("Total escaneado: L. " + total);
                                });
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Fallo OCR en vivo", e))
                    .addOnCompleteListener(task -> {
                        activity.isProcessing.set(false);
                        imageProxy.close();
                    });
        } else {
            activity.isProcessing.set(false);
            imageProxy.close();
        }
    }

    public String extractTotal(String text) {
        // Basic regex to find numbers after keywords like "Total" or "Monto"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(Total|Monto|Importe|SUMA)\\s*[:$]?\\s*([\\d.,]+)",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(2); // Returns the numeric part
        }
        return "No encontrado";
    }
}
