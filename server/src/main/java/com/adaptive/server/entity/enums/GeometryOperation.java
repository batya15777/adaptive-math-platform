package com.adaptive.server.entity.enums;

import java.util.Arrays;
import java.util.List;

public enum GeometryOperation {

    RECTANGLE_AREA("rectangle_area") {
        @Override public String buildQuestion(int w, int h, int b, int r) {
            return "Find the area of a rectangle with width " + w + " and height " + h;
        }
        @Override public double compute(int w, int h, int b, int r) { return (double) w * h; }
        @Override public List buildSolution(int w, int h, int b, int r) {
            double area = compute(w, h, b, r);
            return List.of(w + " * " + h + " = " + fmt(area), "Area = " + fmt(area));
        }
    },

    RECTANGLE_PERIMETER("rectangle_perimeter") {
        @Override public String buildQuestion(int w, int h, int b, int r) {
            return "Find the perimeter of a rectangle with width " + w + " and height " + h;
        }
        @Override public double compute(int w, int h, int b, int r) { return 2.0 * (w + h); }
        @Override public List buildSolution(int w, int h, int b, int r) {
            double p = compute(w, h, b, r);
            return List.of("2 * (" + w + " + " + h + ") = " + fmt(p), "Perimeter = " + fmt(p));
        }
    },

    TRIANGLE_AREA("triangle_area") {
        @Override public String buildQuestion(int w, int h, int b, int r) {
            return "Find the area of a triangle with base " + b + " and height " + h;
        }
        @Override public double compute(int w, int h, int b, int r) { return (b * h) / 2.0; }
        @Override public List buildSolution(int w, int h, int b, int r) {
            double area = compute(w, h, b, r);
            return List.of("(" + b + " * " + h + ") / 2 = " + fmt(area), "Area = " + fmt(area));
        }
    },

    CIRCLE_AREA("circle_area") {
        @Override public String buildQuestion(int w, int h, int b, int r) {
            return "Find the area of a circle with radius " + r + " (use π ≈ 3.14)";
        }
        @Override public double compute(int w, int h, int b, int r) { return 3.14 * r * r; }
        @Override public List buildSolution(int w, int h, int b, int r) {
            double area = compute(w, h, b, r);
            return List.of("3.14 * " + r + "² = 3.14 * " + (r * r) + " = " + fmt(area), "Area = " + fmt(area));
        }
    };

    private final String subSubjectName;

    GeometryOperation(String subSubjectName) {
        this.subSubjectName = subSubjectName;
    }

    public String getSubSubjectName() { return subSubjectName; }

    /** Builds the word-problem question string. */
    public abstract String buildQuestion(int w, int h, int b, int r);

    /** Computes the numeric answer. */
    public abstract double compute(int w, int h, int b, int r);

    /** Returns solution steps as a List<String>. */
    public abstract List<String> buildSolution(int w, int h, int b, int r);

    public static GeometryOperation from(String value) {
        return Arrays.stream(values())
                .filter(op -> op.name().equalsIgnoreCase(value) || op.subSubjectName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown geometry operation: '" + value + "'"));
    }

    private static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }
}
