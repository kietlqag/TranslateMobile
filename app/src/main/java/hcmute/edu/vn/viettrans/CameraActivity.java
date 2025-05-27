package hcmute.edu.vn.viettrans;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";

    private PreviewView previewView;
    private ImageButton backButton;
    private FloatingActionButton captureButton;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initViews();
        setupClickListeners();
        initTextRecognizer();
        startCamera();
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        backButton = findViewById(R.id.backButton);
        captureButton = findViewById(R.id.captureButton);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        captureButton.setOnClickListener(v -> captureImage());
    }

    private void initTextRecognizer() {
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
                Toast.makeText(this, "Không thể khởi tạo camera", Toast.LENGTH_SHORT).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        // Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image capture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // Camera selector
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll();

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
            );

        } catch (Exception e) {
            Log.e(TAG, "Camera binding failed", e);
            Toast.makeText(this, "Lỗi khi kết nối camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void captureImage() {
        if (imageCapture == null) {
            return;
        }

        captureButton.setEnabled(false);

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                new java.io.File(getCacheDir(), "temp_image.jpg")
        ).build();

        imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        // Process the captured image
                        processImage(new java.io.File(getCacheDir(), "temp_image.jpg"));
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Image capture failed", exception);
                        Toast.makeText(CameraActivity.this, "Lỗi khi chụp ảnh",
                                Toast.LENGTH_SHORT).show();
                        captureButton.setEnabled(true);
                    }
                }
        );
    }

    private void processImage(java.io.File imageFile) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (bitmap != null) {
                InputImage image = InputImage.fromBitmap(bitmap, 0);
                recognizeTextWithGoogleCloud(bitmap);
//                recognizeText(image);
            } else {
                Toast.makeText(this, "Không thể đọc ảnh", Toast.LENGTH_SHORT).show();
                captureButton.setEnabled(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(this, "Lỗi khi xử lý ảnh", Toast.LENGTH_SHORT).show();
            captureButton.setEnabled(true);
        }
    }

    private void recognizeText(InputImage image) {
        Task<Text> result = textRecognizer.process(image)
                .addOnSuccessListener(text -> {
                    String extractedText = text.getText();
                    if (extractedText.trim().isEmpty()) {
                        Toast.makeText(this, "Không tìm thấy văn bản trong ảnh",
                                Toast.LENGTH_SHORT).show();
                        captureButton.setEnabled(true);
                    } else {
                        // Return the extracted text
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("extracted_text", extractedText);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Text recognition failed", e);
                    Toast.makeText(this, "Lỗi khi nhận diện văn bản", Toast.LENGTH_SHORT).show();
                    captureButton.setEnabled(true);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textRecognizer != null) {
            textRecognizer.close();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
    private void recognizeTextWithGoogleCloud(Bitmap bitmap) {
        captureButton.setEnabled(false);

        // Convert bitmap to Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        // Build JSON request
        JSONObject requestJson = new JSONObject();
        try {
            JSONObject image = new JSONObject();
            image.put("content", base64Image);

            JSONObject feature = new JSONObject();
            feature.put("type", "TEXT_DETECTION");

            JSONArray featuresArray = new JSONArray();
            featuresArray.put(feature);

            JSONObject request = new JSONObject();
            request.put("image", image);
            request.put("features", featuresArray);

            JSONArray requestsArray = new JSONArray();
            requestsArray.put(request);

            requestJson.put("requests", requestsArray);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi tạo JSON yêu cầu", Toast.LENGTH_SHORT).show();
            captureButton.setEnabled(true);
            return;
        }

        // GET Google OCR API
        String apiKey = "YOUR_API_KEY";
        String url = "https://vision.googleapis.com/v1/images:annotate?key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, requestJson,
                response -> {
                    try {
                        JSONArray responses = response.getJSONArray("responses");
                        JSONObject annotation = responses.getJSONObject(0);

                        if (!annotation.has("fullTextAnnotation")) {
                            Toast.makeText(this, "Không tìm thấy văn bản", Toast.LENGTH_SHORT).show();
                            captureButton.setEnabled(true);
                            return;
                        }

                        String text = annotation.getJSONObject("fullTextAnnotation").getString("text");

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("extracted_text", text);
                        setResult(RESULT_OK, resultIntent);
                        finish();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Lỗi khi xử lý kết quả OCR", Toast.LENGTH_SHORT).show();
                        captureButton.setEnabled(true);
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Lỗi khi gọi Google Vision API", Toast.LENGTH_SHORT).show();
                    captureButton.setEnabled(true);
                });

        queue.add(jsonObjectRequest);
    }
}


