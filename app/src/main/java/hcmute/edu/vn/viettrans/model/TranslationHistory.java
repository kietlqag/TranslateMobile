package hcmute.edu.vn.viettrans.model;

public class TranslationHistory {
    private String originalText;
    private String translatedText;
    private String fromLanguage;
    private String toLanguage;
    private String timestamp;

    public TranslationHistory(String originalText, String translatedText,
                              String fromLanguage, String toLanguage, String timestamp) {
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.fromLanguage = fromLanguage;
        this.toLanguage = toLanguage;
        this.timestamp = timestamp;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    public String getFromLanguage() {
        return fromLanguage;
    }

    public void setFromLanguage(String fromLanguage) {
        this.fromLanguage = fromLanguage;
    }

    public String getToLanguage() {
        return toLanguage;
    }

    public void setToLanguage(String toLanguage) {
        this.toLanguage = toLanguage;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getLanguagePair() {
        return fromLanguage + " → " + toLanguage;
    }
}
