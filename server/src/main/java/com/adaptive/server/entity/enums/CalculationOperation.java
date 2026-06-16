package com.adaptive.server.entity.enums;

import java.util.Arrays;

/**
 * The four sub-subjects of the "Calculation" subject.
 * Each value knows its sub_subject name in the DB, its math symbol,
 * and a default template used when no templates are seeded yet.
 */
public enum CalculationOperation {

    ADD("add", "+"),
    SUB("sub", "-"),
    MULT("mult", "*"),
    DIV("div", "/"),

    /**
     * Combines several operations in one expression (e.g. add and mult together).
     */
    MIXED("mixed", null) {
        @Override
        public String getDefaultTemplate() {
            return "X + X * X";
        }
    };

    private final String subSubjectName;
    private final String symbol;

    CalculationOperation(String subSubjectName, String symbol) {
        this.subSubjectName = subSubjectName;
        this.symbol = symbol;
    }

    public String getSubSubjectName() {
        return subSubjectName;
    }

    /**
     * The math symbol of this operation, or null for MIXED (which has several).
     */
    public String getSymbol() {
        return symbol;
    }

    public String getDefaultTemplate() {
        return "X " + symbol + " X";
    }

    /**
     * Case-insensitive lookup by enum name or sub-subject name
     * (e.g. "add", "ADD", "mult"). Useful for parsing request params.
     */
    public static CalculationOperation from(String value) {
        return Arrays.stream(values())
                .filter(op -> op.name().equalsIgnoreCase(value) || op.subSubjectName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown calculation operation: '" + value + "'. Expected one of: add, sub, mult, div, mixed"));
    }
}
