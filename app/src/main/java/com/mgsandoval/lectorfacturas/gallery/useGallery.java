package com.mgsandoval.lectorfacturas.gallery;

import static com.mgsandoval.lectorfacturas.MainActivity.TAG;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.mlkit.vision.common.InputImage;
import com.mgsandoval.lectorfacturas.MainActivity;

public class useGallery {

    private final MainActivity activity;
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    public useGallery(MainActivity activity, ActivityResultLauncher<PickVisualMediaRequest> pickMedia) {
        this.activity = activity;
        this.pickMedia = pickMedia;
    }

    public void openGallery() {
        if (activity.scanner != null) {

        }

        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    public void analizarImagenEstatica(InputImage image) {
        activity.tvResultados.setText("Analizando imagen...");
        Log.e("useGallery", "Analizando imagen...");
        activity.textRecognizer.process(image)
                .addOnSuccessListener(labels -> activity.runOnUiThread(() -> activity.mostrarResultadosImagen(labels)))
                .addOnFailureListener(e -> {
                    activity.tvResultados.setText("Error al analizar la imagen");
                    Log.e("useGallery", "Error al analizar la imagen", e);
                });
    }
}
