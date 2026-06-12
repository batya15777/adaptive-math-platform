package com.adaptive.server.utils;

import com.adaptive.server.entity.QuestionTemplate;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.Subject;
import com.adaptive.server.entity.enums.CalculationOperation;
import com.adaptive.server.repository.QuestionTemplateRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import com.adaptive.server.repository.SubjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the database with initial data for manual testing.
 * Disabled by default — enable by adding to application.properties:
 *
 *   app.seed-data=true
 *
 * Safe to run repeatedly: existing data is never duplicated.
 */
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String CALCULATION_SUBJECT = "Calculation";

    private final SubjectRepository subjectRepository;
    private final SubSubjectRepository subSubjectRepository;
    private final QuestionTemplateRepository templateRepository;

    public DataInitializer(SubjectRepository subjectRepository,
                           SubSubjectRepository subSubjectRepository,
                           QuestionTemplateRepository templateRepository) {
        this.subjectRepository = subjectRepository;
        this.subSubjectRepository = subSubjectRepository;
        this.templateRepository = templateRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Seeding initial data for manual tests...");
        Subject calculation = getOrCreateSubject(CALCULATION_SUBJECT);

        for (CalculationOperation operation : CalculationOperation.values()) {
            SubSubject subSubject = getOrCreateSubSubject(operation.getSubSubjectName(), calculation);
            seedTemplates(subSubject, operation);
        }
        log.info("Seeding done.");
    }

    private Subject getOrCreateSubject(String name) {
        return subjectRepository.findByName(name)
                .orElseGet(() -> {
                    log.info("Creating subject '{}'", name);
                    return subjectRepository.save(new Subject(name));
                });
    }

    private SubSubject getOrCreateSubSubject(String name, Subject subject) {
        SubSubject existing = subSubjectRepository.findByNameAndSubject_Name(name, subject.getName());
        if (existing != null) {
            return existing;
        }
        log.info("Creating sub-subject '{}' under '{}'", name, subject.getName());
        return subSubjectRepository.save(new SubSubject(name, subject));
    }

    /**
     * Three difficulty levels per operation. Single operations are built from
     * their symbol ("X + X", "X + X + X", "(X + X) + X"); MIXED combines them.
     */
    private void seedTemplates(SubSubject subSubject, CalculationOperation operation) {
        if (!templateRepository.findAllBySubSubject_Name(subSubject.getName()).isEmpty()) {
            return; // already seeded
        }
        if (operation == CalculationOperation.MIXED) {
            saveTemplate(subSubject, "X + X * X", 1);
            saveTemplate(subSubject, "X * X - X + X", 2);
            saveTemplate(subSubject, "(X + X) * X - X / X", 3);
        } else {
            String s = operation.getSymbol();
            saveTemplate(subSubject, "X " + s + " X", 1);
            saveTemplate(subSubject, "X " + s + " X " + s + " X", 2);
            saveTemplate(subSubject, "(X " + s + " X) " + s + " X", 3);
        }
        log.info("Created 3 templates for '{}'", subSubject.getName());
    }

    private void saveTemplate(SubSubject subSubject, String expression, int difficulty) {
        QuestionTemplate template = new QuestionTemplate(subSubject, expression);
        template.setDifficultyLevel(difficulty);
        templateRepository.save(template);
    }
}
