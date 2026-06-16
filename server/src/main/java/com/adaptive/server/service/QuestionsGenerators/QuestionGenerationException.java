package com.adaptive.server.service.QuestionsGenerators;

/**
 * Thrown when a question cannot be generated (missing sub-subject, no templates, etc.).
 */
public class QuestionGenerationException extends RuntimeException {

    public QuestionGenerationException(String message) {
        super(message);
    }
}
