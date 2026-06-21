package com.adaptive.server.DTOs;

public class AdminErrorStatDto {
    private final String errorPattern;
    private final long occurrences;

    public AdminErrorStatDto(String errorPattern, long occurrences) {
        this.errorPattern = errorPattern;
        this.occurrences = occurrences;
    }

    public String getErrorPattern() { return errorPattern; }
    public long getOccurrences() { return occurrences; }
}
