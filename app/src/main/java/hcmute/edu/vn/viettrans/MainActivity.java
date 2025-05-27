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
import android.view.Menu;
import android.view.MenuItem;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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

import hcmute.edu.vn.viettrans.adapter.HistoryListAdapter;
import hcmute.edu.vn.viettrans.database.TranslationHistoryDAO;
import hcmute.edu.vn.viettrans.model.Language;
import hcmute.edu.vn.viettrans.model.TranslationHistory;
import hcmute.edu.vn.viettrans.utils.SettingsManager;

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
    private HistoryListAdapter historyAdapter;
    private RequestQueue requestQueue;
    private TranslationHistoryDAO historyDAO;
    private SettingsManager settingsManager;

    // Constants
    private static final String GOOGLE_TRANSLATE_API_KEY = "YOUR_API_KEY";
    private static final String TRANSLATE_URL = "https://translation.googleapis.com/language/translate/v2";
    private static final String DETECT_URL = "https://translation.googleapis.com/language/translate/v2/detect";

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> speechLauncher;
    private ActivityResultLauncher<String> micPermissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    private boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupLanguages();
        setupSpinners();
        setupClickListeners();
        setupActivityResultLaunchers();

        requestQueue = Volley.newRequestQueue(this);
        historyList = new ArrayList<>();
        historyDAO = new TranslationHistoryDAO(this);
        settingsManager = new SettingsManager(this);
        setupRecyclerView();
        loadTranslationHistory();
        applySettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTranslationHistory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (historyDAO != null) {
            historyDAO.close();
        }
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
        if (historyList == null) {
            historyList = new ArrayList<>();
        }
        historyAdapter = new HistoryListAdapter(historyList, this::onHistoryItemClick);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(historyAdapter);
        historyRecyclerView.setNestedScrollingEnabled(true);
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
            if (settingsManager.isShowNotificationsEnabled()) {
                Toast.makeText(this, "Vui lòng nhập văn bản cần dịch", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String fromLang = languages.get(fromLanguageSpinner.getSelectedItemPosition()).getCode();
        String toLang = languages.get(toLanguageSpinner.getSelectedItemPosition()).getCode();

        if (fromLang.equals(toLang) && !fromLang.equals("auto")) {
            if (settingsManager.isShowNotificationsEnabled()) {
                Toast.makeText(this, "Ngôn ngữ nguồn và đích không thể giống nhau", Toast.LENGTH_SHORT).show();
            }
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
                String selectedLangCode = languages.get(fromLanguageSpinner.getSelectedItemPosition()).getCode();
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLangCode);
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
        if (!settingsManager.isSaveHistoryEnabled()) {
            return;
        }

        try {
            String fromLangName = getLanguageName(fromLang);
            String toLangName = getLanguageName(toLang);

            TranslationHistory history = new TranslationHistory(
                    originalText, translatedText, fromLangName, toLangName);

            // Save to database
            historyDAO.open();
            historyDAO.insertHistory(history);
            historyDAO.close();

            // Reload history from database
            loadTranslationHistory();
        } catch (Exception e) {
            e.printStackTrace();
            if (historyDAO != null) {
                historyDAO.close();
            }
        }
    }

    private void onHistoryItemClick(TranslationHistory history) {
        inputText.setText(history.getSourceText());
        outputText.setText(history.getTranslatedText());

        // Set spinners to match history item
        setSpinnerSelection(fromLanguageSpinner, history.getSourceLang());
        setSpinnerSelection(toLanguageSpinner, history.getTargetLang());
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

    private void loadTranslationHistory() {
        try {
            historyDAO.open();
            List<TranslationHistory> histories = historyDAO.getAllHistory();
            historyList.clear();
            historyList.addAll(histories);
            historyDAO.close();

            // Show history section if there are items
            if (!historyList.isEmpty()) {
                historyLabel.setVisibility(View.VISIBLE);
                if (historyAdapter != null) {
                    historyAdapter.notifyDataSetChanged();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (historyDAO != null) {
                historyDAO.close();
            }
        }
    }

    private void applySettings() {
        // Apply font size
        int fontSize = settingsManager.getFontSize();
        inputText.setTextSize(fontSize);
        outputText.setTextSize(fontSize);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_theme) {
            toggleTheme();
            return true;
        } else if (id == R.id.action_clear_history) {
            showClearHistoryDialog();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (id == R.id.action_settings) {
            showSettingsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showClearHistoryDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Xóa lịch sử")
            .setMessage("Bạn có chắc muốn xóa toàn bộ lịch sử dịch?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                historyDAO.open();
                historyDAO.clearAllHistory();
                historyDAO.close();
                historyList.clear();
                historyAdapter.notifyDataSetChanged();
                historyLabel.setVisibility(View.GONE);
                Toast.makeText(this, "Đã xóa lịch sử", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Giới thiệu")
            .setMessage("Viet Translate\nPhiên bản 1.0\n\nỨng dụng dịch văn bản đa ngôn ngữ với các tính năng:\n" +
                    "- Dịch văn bản giữa nhiều ngôn ngữ\n" +
                    "- Nhận dạng văn bản từ ảnh\n" +
                    "- Nhận dạng giọng nói\n" +
                    "- Lưu lịch sử dịch\n" +
                    "- Hỗ trợ chế độ sáng/tối")
            .setPositiveButton("OK", null)
            .show();
    }

    private void showSettingsDialog() {
        boolean[] checkedItems = {
            settingsManager.isSaveHistoryEnabled(),
            settingsManager.isShowNotificationsEnabled()
        };

        new AlertDialog.Builder(this)
            .setTitle("Cài đặt")
            .setMultiChoiceItems(
                new String[]{
                    "Lưu lịch sử dịch",
                    "Hiển thị thông báo"
                },
                checkedItems,
                (dialog, which, isChecked) -> {
                    switch (which) {
                        case 0:
                            settingsManager.setSaveHistoryEnabled(isChecked);
                            break;
                        case 1:
                            settingsManager.setShowNotificationsEnabled(isChecked);
                            break;
                    }
                }
            )
            .setPositiveButton("OK", (dialog, which) -> {
                applySettings();
                Toast.makeText(this, "Đã cập nhật cài đặt", Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton("Cỡ chữ", (dialog, which) -> showFontSizeDialog())
            .show();
    }

    private void showFontSizeDialog() {
        String[] sizes = {"Nhỏ", "Vừa", "Lớn", "Rất lớn"};
        int currentSize = settingsManager.getFontSize();
        int selectedIndex = 0;
        
        if (currentSize <= 14) selectedIndex = 0;
        else if (currentSize <= 16) selectedIndex = 1;
        else if (currentSize <= 18) selectedIndex = 2;
        else selectedIndex = 3;

        new AlertDialog.Builder(this)
            .setTitle("Chọn cỡ chữ")
            .setSingleChoiceItems(sizes, selectedIndex, (dialog, which) -> {
                int newSize;
                switch (which) {
                    case 0: newSize = 14; break;
                    case 1: newSize = 16; break;
                    case 2: newSize = 18; break;
                    case 3: newSize = 20; break;
                    default: newSize = 16;
                }
                settingsManager.setFontSize(newSize);
                applySettings();
            })
            .setPositiveButton("OK", null)
            .show();
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}