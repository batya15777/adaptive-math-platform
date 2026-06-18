package com.adaptive.server.entity.enums;

public enum ProfileTheme {
    DARK("Dark"),
    LIGHT("Light");

    private final String label;

    ProfileTheme(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
