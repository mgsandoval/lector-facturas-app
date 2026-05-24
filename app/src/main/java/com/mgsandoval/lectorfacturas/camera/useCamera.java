package com.mgsandoval.lectorfacturas.camera;

import static com.mgsandoval.lectorfacturas.MainActivity.TAG;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.mgsandoval.lectorfacturas.MainActivity;
import com.mgsandoval.lectorfacturas.gallery.useGallery;
import com.mgsandoval.lectorfacturas.scanner.useScanner;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class useCamera {

    private final MainActivity activity;
    private final ActivityResultLauncher<String> requestPermissionLauncher;

    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    public boolean isAnalysisRunning = false;

    public useScanner scanner;
    public useGallery gallery;

    public useCamera(MainActivity activity, ActivityResultLauncher<String> requestPermissionLauncher) {
        this.activity = activity;
        this.requestPermissionLauncher = requestPermissionLauncher;
    }

    public void handleCameraPermission() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(activity);
        cameraProviderFuture.addListener(() -> {
            try {
                activity.cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e("useCamera", "Error al iniciar cámara", e);
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    private void bindCameraUseCases() {
        if (activity.cameraProvider == null) return;
        activity.cameraProvider.unbindAll();

        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();
        preview.setSurfaceProvider(activity.previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        imageCapture = new ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();

        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        if(scanner != null) {
            imageAnalysis.setAnalyzer(activity.cameraExecutor, scanner::processImageProxy);
        }

        try {
            if (isAnalysisRunning) {
                activity.cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageCapture, imageAnalysis);
            } else {
                activity.cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageCapture);
            }
        } catch (Exception e) {
            Log.e("useCamera", "Error al vincular casos de uso", e);
        }
    }

    public void takePhoto() {
        if (imageCapture == null) return;
        if (isAnalysisRunning) scanner.toggleAnalysis();

        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VisualExplorer");
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(activity.getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                .build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(activity),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = outputFileResults.getSavedUri();
                        Toast.makeText(activity, "¡Foto guardada!", Toast.LENGTH_SHORT).show();
                        try {
                            InputImage image = InputImage.fromFilePath(activity, savedUri);
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), savedUri);
                            activity.ivImagen.setImageBitmap(bitmap);
                            activity.setAppState(MainActivity.AppState.IMAGE_DISPLAY);
                            gallery.analizarImagenEstatica(image);
                        } catch (IOException e) {
                            Log.e("useCamera", "Error procesando imagen guardada ", e);
                        }
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("useCamera", "Error al tomar foto: ", exception);
                    }
                });
    }
}
