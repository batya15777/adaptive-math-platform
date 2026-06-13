package com.adaptive.server.service.QuestionsGenerators;

import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.repository.QuestionTemplateRepository;
import com.adaptive.server.repository.SubSubjectRepository;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Base class for question generators. Each generator handles one Subject
 * and declares an enum T listing the sub-subjects it can generate for.
 */
public abstract class QuestionGenerator<T extends Enum<T>> {

    protected final QuestionTemplateRepository templateRepository;
    protected final SubSubjectRepository subSubjectRepository;

    public QuestionGenerator(QuestionTemplateRepository templateRepository, SubSubjectRepository subSubjectRepository) {
        this.templateRepository = templateRepository;
        this.subSubjectRepository = subSubjectRepository;
    }

    public abstract Question createQuestion(T subSubject, int difficultyLevel, String language, boolean multipleChoice);

    /**
     * The Subject this generator handles (e.g. "Calculation").
     */
    protected abstract String getSubjectName();

    /**
     * Looks up a sub-subject by name, scoped under this generator's subject.
     */
    protected SubSubject resolveSubSubject(String subSubjectName) {
        SubSubject subSubject = subSubjectRepository.findByNameAndSubject_Name(subSubjectName, getSubjectName());
        if (subSubject == null) {
            throw new QuestionGenerationException(
                    "SubSubject '" + subSubjectName + "' not found under subject '" + getSubjectName() + "'");
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
