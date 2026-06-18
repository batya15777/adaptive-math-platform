package com.adaptive.server.entity.enums;

public enum Language {
    HEBREW("Hebrew (עברית)"),
    ENGLISH("English"),
    RUSSIAN("Russian (Русский)");

    private final String label;

    Language(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
