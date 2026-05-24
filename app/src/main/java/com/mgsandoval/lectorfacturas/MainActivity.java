package com.mgsandoval.lectorfacturas;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.translation.Translator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.camera.view.PreviewView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import com.mgsandoval.lectorfacturas.camera.useCamera;
import com.mgsandoval.lectorfacturas.gallery.useGallery;
import com.mgsandoval.lectorfacturas.scanner.useScanner;

import java.io.IOException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final float CONFIDENCE_THRESHOLD = 0.85f;
    public static final String TAG = "MainActivity";

    public useCamera camera;
    public useGallery gallery;
    public useScanner scanner;
    public ImageView ivImagen;
    public ImageButton btnCloseImage, btnAbrirGaleria, btnTomarFoto, btnAnalizar;
    public TextView tvResultados, tvDatoCurioso;
    private CardView cardResultados;
    public PreviewView previewView;

    public TextRecognizer textRecognizer;
    public ProcessCameraProvider cameraProvider;
    public TextToSpeech tts;
    public Translator translatorEnToEs;
    public ExecutorService cameraExecutor;

    public final AtomicBoolean isProcessing = new AtomicBoolean(false);
    public enum AppState { LIVE_CAMERA, IMAGE_DISPLAY }

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
        if (uri != null) {
            try {
                InputImage image = InputImage.fromFilePath(this, uri);
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                ivImagen.setImageBitmap(bitmap);
                setAppState(AppState.IMAGE_DISPLAY);
                gallery.analizarImagenEstatica(image);
            } catch (IOException e) {
                Log.e(TAG, "Error al cargar imagen de galería", e);
            }
        }
    });

    private final ActivityResultLauncher<String> requestpermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if(isGranted) camera.startCamera();
                else Toast.makeText(this, "¡Necesito permiso a la cámara!", Toast.LENGTH_LONG).show();
            }
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        ivImagen = findViewById(R.id.ivImagen);
        btnCloseImage = findViewById(R.id.btnCloseImage);
        btnAbrirGaleria = findViewById(R.id.btnAbrirGaleria);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnAnalizar = findViewById(R.id.btnAnalizar);
        tvResultados = findViewById(R.id.tvResultados);
        tvDatoCurioso = findViewById(R.id.tvDatoCurioso);
        cardResultados = findViewById(R.id.cardResultados);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        cameraExecutor = Executors.newSingleThreadExecutor();

        camera = new useCamera(this, requestpermissionLauncher);
        gallery = new useGallery(this, pickMedia);
        scanner = new useScanner(this);

        camera.scanner = scanner;
        camera.gallery = gallery;

        camera.handleCameraPermission();

        btnAnalizar.setOnClickListener(v -> scanner.toggleAnalysis());
        btnTomarFoto.setOnClickListener(v -> camera.takePhoto());
        btnAbrirGaleria.setOnClickListener(v -> gallery.openGallery());
        btnCloseImage.setOnClickListener(v -> setAppState(AppState.LIVE_CAMERA));

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();
    }

    public void mostrarResultadosImagen(Text visionText) {
        tvDatoCurioso.setVisibility(View.GONE);
        String fullText = visionText.getText();
        if (fullText.isEmpty()) {
            tvResultados.setText("No reconozco nada. Intenta de nuevo.");
            return;
        }

        // Use your scanner to extract the total amount
        String totalDetectado = scanner.extractTotal(fullText);

        // Update the UI
        tvResultados.setText("Factura Escaneada\nTotal Detectado: L. " + totalDetectado);

        // TODO: Here you could trigger a Dialog to edit/confirm the 'totalDetectado'
        // before sending it to your NodeJS server.

        showConfirmationDialog(totalDetectado);
    }

    private void showConfirmationDialog(String total) {
        // Create an EditText for the user to modify the value
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(total.equals("No encontrado") ? "" : total);
        input.setHint("0.00");

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Confirmar Factura")
                .setMessage("Verifica el monto total detectado:")
                .setView(input)
                .setPositiveButton("Enviar a Node.js", (dialog, which) -> {
                    String finalAmount = input.getText().toString();
                    enviarABackend(finalAmount);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void enviarABackend(String amount) {
        Toast.makeText(this, "Enviando $" + amount + " al servidor...", Toast.LENGTH_SHORT).show();
        // TODO: Implement your Retrofit call here to connect to Node.js/PostgreSQL
    }

    public void setAppState(AppState state) {
        if (state == AppState.LIVE_CAMERA) {
            previewView.setVisibility(View.VISIBLE);
            ivImagen.setVisibility(View.GONE);
            btnCloseImage.setVisibility(View.GONE);
            tvResultados.setText("Elige una opción abajo");
            tvDatoCurioso.setVisibility(View.GONE);
        } else {
            previewView.setVisibility(View.GONE);
            ivImagen.setVisibility(View.VISIBLE);
            btnCloseImage.setVisibility(View.VISIBLE);
        }
    }
}
