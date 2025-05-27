package hcmute.edu.vn.viettrans.model;

public class TranslationHistory {
    private long id;
    private String sourceText;
    private String translatedText;
    private String sourceLang;
    private String targetLang;
    private String timestamp;

    public TranslationHistory() {}

    public TranslationHistory(String sourceText, String translatedText, String sourceLang, String targetLang) {
        this.sourceText = sourceText;
        this.translatedText = translatedText;
        this.sourceLang = sourceLang;
        this.targetLang = targetLang;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSourceText() { return sourceText; }
    public void setSourceText(String sourceText) { this.sourceText = sourceText; }

    public String getTranslatedText() { return translatedText; }
    public void setTranslatedText(String translatedText) { this.translatedText = translatedText; }

    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }

    public String getTargetLang() { return targetLang; }
    public void setTargetLang(String targetLang) { this.targetLang = targetLang; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getLanguagePair() {
        return sourceLang + " → " + targetLang;
    }
}
