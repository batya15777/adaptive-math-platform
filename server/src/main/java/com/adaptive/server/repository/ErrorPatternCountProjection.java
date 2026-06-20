package com.adaptive.server.repository;

/**
 * How often a student hit each error pattern (wrong answers only), most frequent first.
 * Used as a fallback signal for adaptive recommendations when no cluster is available.
 */
public interface ErrorPatternCountProjection {
    String getErrorPattern();
    Long getOccurrences();
}
