package com.adaptive.server.entity.enums;

/**
 * Represents the current movement state of the student's spaceship
 * in the gamified progress visualization.
 *
 * MOVING   – student is progressing normally at their current level.
 * STOPPED  – student is struggling (< 60% in last 10 answers); spaceship pauses.
 * BOOSTING – student just leveled up (≥ 80% in last 30 answers); spaceship surges forward.
 */
public enum SpaceshipStatus {
    MOVING,
    STOPPED,
    BOOSTING
}
