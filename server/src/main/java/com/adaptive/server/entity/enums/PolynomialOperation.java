package com.adaptive.server.entity.enums;

import java.util.Arrays;

public enum PolynomialOperation {

    LINEAR("linear"),
    QUADRATIC("quadratic");

    private final String subSubjectName;

    PolynomialOperation(String subSubjectName) {
        this.subSubjectName = subSubjectName;
    }

    public String getSubSubjectName() { return subSubjectName; }

    public static PolynomialOperation from(String value) {
        return Arrays.stream(values())
                .filter(op -> op.name().equalsIgnoreCase(value) || op.subSubjectName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown polynomial operation: '" + value + "'"));
    }
}
