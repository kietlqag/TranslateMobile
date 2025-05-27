package hcmute.edu.vn.viettrans;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.view.translation.Translator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.mlkit.nl.languageid.LanguageIdentifier;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hcmute.edu.vn.viettrans.model.Language;
import hcmute.edu.vn.viettrans.model.TranslationHistory;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private Spinner fromLanguageSpinner, toLanguageSpinner;
    private EditText inputText;
    private TextView outputText, detectedLanguageText, historyLabel;
    private Button translateButton, clearButton;
    private ImageButton cameraButton, micButton, swapLanguagesButton, copyButton;
    private ProgressBar progressBar;
    private RecyclerView historyRecyclerView;

    // Data
    private List<Language> languages;
    private List<TranslationHistory> historyList;
    private TranslationHistoryAdapter historyAdapter;
    private RequestQueue requestQueue;

    // Constants
    private static final String GOOGLE_TRANSLATE_API_KEY = "YOUR_GOOGLETRANSLATE_API";
    private static final String TRANSLATE_URL = "https://translation.googleapis.com/language/translate/v2";
    private static final String DETECT_URL = "https://translation.googleapis.com/language/translate/v2/detect";

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> speechLauncher;
    private ActivityResultLauncher<String> micPermissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupLanguages();
        setupSpinners();
        setupRecyclerView();
        setupClickListeners();
        setupActivityResultLaunchers();

        requestQueue = Volley.newRequestQueue(this);
        historyList = new ArrayList<>();
    }

    private void initViews() {
        fromLanguageSpinner = findViewById(R.id.fromLanguageSpinner);
        toLanguageSpinner = findViewById(R.id.toLanguageSpinner);
        inputText = findViewById(R.id.inputText);
        outputText = findViewById(R.id.outputText);
        detectedLanguageText = findViewById(R.id.detectedLanguageText);
        historyLabel = findViewById(R.id.historyLabel);
        translateButton = findViewById(R.id.translateButton);
        clearButton = findViewById(R.id.clearButton);
        cameraButton = findViewById(R.id.cameraButton);
        micButton = findViewById(R.id.micButton);
        swapLanguagesButton = findViewById(R.id.swapLanguagesButton);
        copyButton = findViewById(R.id.copyButton);
        progressBar = findViewById(R.id.progressBar);
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
    }

    private void setupLanguages() {
        languages = new ArrayList<>();
        languages.add(new Language("auto", "Phát hiện tự động"));
        languages.add(new Language("vi", "Tiếng Việt"));
        languages.add(new Language("en", "English"));
        languages.add(new Language("zh", "中文"));
        languages.add(new Language("ja", "日本語"));
        languages.add(new Language("ko", "한국어"));
        languages.add(new Language("fr", "Français"));
        languages.add(new Language("de", "Deutsch"));
        languages.add(new Language("es", "Español"));
        languages.add(new Language("ru", "Русский"));
        languages.add(new Language("th", "ไทย"));
        languages.add(new Language("id", "Bahasa Indonesia"));
    }

    private void setupSpinners() {
        List<String> languageNames = new ArrayList<>();
        for (Language lang : languages) {
            languageNames.add(lang.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, languageNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        fromLanguageSpinner.setAdapter(adapter);
        toLanguageSpinner.setAdapter(adapter);

        // Set default selections
        fromLanguageSpinner.setSelection(0); // Auto detect
        toLanguageSpinner.setSelection(2); // English

        fromLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // Auto detect selected
                    detectLanguage();
                } else {
                    detectedLanguageText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        historyAdapter = new TranslationHistoryAdapter(historyList, this::onHistoryItemClick);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(historyAdapter);
    }

    private void setupClickListeners() {
        translateButton.setOnClickListener(v -> translateText());
        clearButton.setOnClickListener(v -> clearText());
        cameraButton.setOnClickListener(v -> openCamera());
        micButton.setOnClickListener(v -> startSpeechRecognition());
        swapLanguagesButton.setOnClickListener(v -> swapLanguages());
        copyButton.setOnClickListener(v -> copyToClipboard());
    }

    private void setupActivityResultLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String extractedText = result.getData().getStringExtra("extracted_text");
                        if (extractedText != null && !extractedText.isEmpty()) {
                            inputText.setText(extractedText);
                        }
                    }
                }
        );

        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> results = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (results != null && !results.isEmpty()) {
                            inputText.setText(results.get(0));
                        }
                    }
                }
        );

        micPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startSpeechRecognition();
                    } else {
                        Toast.makeText(this, "Cần quyền ghi âm để sử dụng tính năng này",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(this, "Cần quyền camera để sử dụng tính năng này",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void translateText() {
        String text = inputText.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập văn bản cần dịch", Toast.LENGTH_SHORT).show();
            return;
        }

        String fromLang = languages.get(fromLanguageSpinner.getSelectedItemPosition()).getCode();
        String toLang = languages.get(toLanguageSpinner.getSelectedItemPosition()).getCode();

        if (fromLang.equals(toLang) && !fromLang.equals("auto")) {
            Toast.makeText(this, "Ngôn ngữ nguồn và đích không thể giống nhau", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("q", text);
            requestBody.put("target", toLang);
            if (!fromLang.equals("auto")) {
                requestBody.put("source", fromLang);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            showProgress(false);
            return;
        }

        String url = TRANSLATE_URL + "?key=" + GOOGLE_TRANSLATE_API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                response -> {
                    showProgress(false);
                    try {
                        JSONObject data = response.getJSONObject("data");
                        JSONArray translations = data.getJSONArray("translations");
                        JSONObject translation = translations.getJSONObject(0);

                        String translatedText = translation.getString("translatedText");
                        outputText.setText(translatedText);

                        addToHistory(text, translatedText, fromLang, toLang);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Lỗi khi xử lý kết quả dịch", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    showProgress(false);
                    Toast.makeText(this, "Lỗi khi dịch: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(request);
    }

    private void detectLanguage() {
        String text = inputText.getText().toString().trim();
        if (text.isEmpty()) {
            detectedLanguageText.setVisibility(View.GONE);
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("q", text);
            requestBody.put("key", GOOGLE_TRANSLATE_API_KEY);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                DETECT_URL,
                requestBody,
                response -> {
                    try {
                        JSONObject data = response.getJSONObject("data");
                        JSONArray detections = data.getJSONArray("detections");
                        JSONArray detection = detections.getJSONArray(0);
                        JSONObject detectedLang = detection.getJSONObject(0);

                        String langCode = detectedLang.getString("language");
                        double confidence = detectedLang.getDouble("confidence");

                        String langName = getLanguageName(langCode);
                        detectedLanguageText.setText(String.format("Phát hiện: %s (%.0f%%)",
                                langName, confidence * 100));
                        detectedLanguageText.setVisibility(View.VISIBLE);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // Ignore detection errors
                }
        );

        requestQueue.add(request);
    }

    private String getLanguageName(String code) {
        for (Language lang : languages) {
            if (lang.getCode().equals(code)) {
                return lang.getName();
            }
        }
        return code;
    }

    private void clearText() {
        inputText.setText("");
        outputText.setText("");
        detectedLanguageText.setVisibility(View.GONE);
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            Intent intent = new Intent(this, CameraActivity.class);
            cameraLauncher.launch(intent);
        }
    }

    private void startSpeechRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói điều gì đó...");

                speechLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Thiết bị không hỗ trợ nhận diện giọng nói",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void swapLanguages() {
        if (fromLanguageSpinner.getSelectedItemPosition() == 0) { // Auto detect
            Toast.makeText(this, "Không thể hoán đổi với chế độ tự động phát hiện",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int fromPos = fromLanguageSpinner.getSelectedItemPosition();
        int toPos = toLanguageSpinner.getSelectedItemPosition();

        fromLanguageSpinner.setSelection(toPos);
        toLanguageSpinner.setSelection(fromPos);

        // Swap text content
        String inputStr = inputText.getText().toString();
        String outputStr = outputText.getText().toString();

        inputText.setText(outputStr);
        outputText.setText(inputStr);
    }

    private void copyToClipboard() {
        String text = outputText.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "Không có gì để sao chép", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Translated Text", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Đã sao chép vào clipboard", Toast.LENGTH_SHORT).show();
    }

    private void addToHistory(String originalText, String translatedText,
                              String fromLang, String toLang) {
        String fromLangName = getLanguageName(fromLang);
        String toLangName = getLanguageName(toLang);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        TranslationHistory history = new TranslationHistory(
                originalText, translatedText, fromLangName, toLangName, timestamp);

        historyList.add(0, history); // Add to beginning
        historyAdapter.notifyItemInserted(0);

        // Show history section if it was hidden
        if (historyLabel.getVisibility() == View.GONE) {
            historyLabel.setVisibility(View.VISIBLE);
        }

        // Limit history to 50 items
        if (historyList.size() > 50) {
            historyList.remove(historyList.size() - 1);
            historyAdapter.notifyItemRemoved(historyList.size());
        }
    }

    private void onHistoryItemClick(TranslationHistory history) {
        inputText.setText(history.getOriginalText());
        outputText.setText(history.getTranslatedText());

        // Set spinners to match history item
        setSpinnerSelection(fromLanguageSpinner, history.getFromLanguage());
        setSpinnerSelection(toLanguageSpinner, history.getToLanguage());
    }

    private void setSpinnerSelection(Spinner spinner, String languageName) {
        for (int i = 0; i < languages.size(); i++) {
            if (languages.get(i).getName().equals(languageName)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        translateButton.setEnabled(!show);
    }
}