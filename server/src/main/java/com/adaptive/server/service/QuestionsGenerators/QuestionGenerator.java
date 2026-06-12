package com.adaptive.server.service.QuestionsGenerators;

import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.repository.QuestionTemplateRepository;
import com.adaptive.server.repository.SubSubjectRepository;

import java.util.concurrent.ThreadLocalRandom;

public abstract class QuestionGenerator {

    protected final QuestionTemplateRepository templateRepository;
    protected final SubSubjectRepository subSubjectRepository;

    public QuestionGenerator(QuestionTemplateRepository templateRepository, SubSubjectRepository subSubjectRepository) {
        this.templateRepository = templateRepository;
        this.subSubjectRepository = subSubjectRepository;
    }

    public abstract Question createQuestion(int difficultyLevel, String language, boolean multipleChoice);

    /**
     * The SubSubject name this generator handles (e.g. "Calculation").
     */
    protected abstract String getSubSubjectName();

    protected SubSubject resolveSubSubject() {
        SubSubject subSubject = subSubjectRepository.findByName(getSubSubjectName());
        if (subSubject == null) {
            throw new QuestionGenerationException("SubSubject not found: " + getSubSubjectName());
        }
        return subSubject;
    }

    /**
     * Random value for an 'X' placeholder. Subclasses can override to change the range.
     */
    protected int generatePlaceholderValue(int difficultyLevel) {
        int min = Math.max(1, difficultyLevel);
        int max = Math.max(min + 1, 8 * difficultyLevel);
        return ThreadLocalRandom.current().nextInt(min, max);
    }
}
