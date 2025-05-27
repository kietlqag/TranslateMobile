package hcmute.edu.vn.viettrans.model;

public class Language {
    private String code;
    private String name;

    public Language(String code, String name) { // Bỏ "void" để đúng cú pháp constructor
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}