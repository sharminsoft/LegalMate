package com.yourname.legalmate.LawyerPortal.Models;

public class DocumentField {
    private String key;
    private String label;
    private String labelBangla;
    private String type; // text, textarea, date, number, dropdown
    private boolean required;
    private String hint;
    private String hintBangla;
    private String[] options; // for dropdown

    public DocumentField() {
        // Empty constructor for Firebase
    }

    public DocumentField(String key, String label, String labelBangla, String type, boolean required) {
        this.key = key;
        this.label = label;
        this.labelBangla = labelBangla;
        this.type = type;
        this.required = required;
    }

    public DocumentField(String key, String label, String labelBangla, String type, boolean required, String hint, String hintBangla) {
        this(key, label, labelBangla, type, required);
        this.hint = hint;
        this.hintBangla = hintBangla;
    }

    public DocumentField(String key, String label, String labelBangla, String type, boolean required, String hint, String hintBangla, String[] options) {
        this(key, label, labelBangla, type, required, hint, hintBangla);
        this.options = options;
    }

    // Getters and Setters
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabelBangla() {
        return labelBangla;
    }

    public void setLabelBangla(String labelBangla) {
        this.labelBangla = labelBangla;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getHintBangla() {
        return hintBangla;
    }

    public void setHintBangla(String hintBangla) {
        this.hintBangla = hintBangla;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }
}