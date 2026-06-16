package com.adaptive.server.utils;

import com.adaptive.server.entity.QuestionTemplate;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.Subject;
import com.adaptive.server.entity.enums.CalculationOperation;
import com.adaptive.server.entity.enums.GeometryOperation;
import com.adaptive.server.entity.enums.PolynomialOperation;
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
 * Disabled by default — enable with: app.seed-data=true (or env SEED_DATA=true)
 * Safe to run repeatedly: existing data is never duplicated.
 */
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SubjectRepository subjectRepository;
    private final SubSubjectRepository subSubjectRepository;
    private final QuestionTemplateRepository templateRepository;

    public DataInitializer(SubjectRepository subjectRepository,
                           SubSubjectRepository subSubjectRepository,
                           QuestionTemplateRepository templateRepository) {
        this.subjectRepository    = subjectRepository;
        this.subSubjectRepository = subSubjectRepository;
        this.templateRepository   = templateRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Seeding initial data...");
        seedCalculation();
        seedGeometry();
        seedPolynomial();
        seedAi();
        log.info("Seeding done.");
    }

    // ── Calculation ──────────────────────────────────────────────────────

    private void seedCalculation() {
        Subject subject = getOrCreateSubject("Calculation");
        for (CalculationOperation op : CalculationOperation.values()) {
            SubSubject sub = getOrCreateSubSubject(op.getSubSubjectName(), subject);
            seedCalculationTemplates(sub, op);
        }
    }

    private void seedCalculationTemplates(SubSubject subSubject, CalculationOperation op) {
        if (!templateRepository.findAllBySubSubject_Name(subSubject.getName()).isEmpty()) return;

        if (op == CalculationOperation.MIXED) {
            saveTemplate(subSubject, "X + X * X", 1);
            saveTemplate(subSubject, "X * X - X + X", 2);
            saveTemplate(subSubject, "(X + X) * X - X / X", 3);
        } else {
            String s = op.getSymbol();
            saveTemplate(subSubject, "X " + s + " X", 1);
            saveTemplate(subSubject, "X " + s + " X " + s + " X", 2);
            saveTemplate(subSubject, "(X " + s + " X) " + s + " X", 3);
        }
        log.info("Created 3 templates for '{}'", subSubject.getName());
    }

    // ── Geometry ─────────────────────────────────────────────────────────
    // GeometryGenerator does not use DB templates (formulas are fixed),
    // but the sub-subject rows must exist as FKs for the Question entity.

    private void seedGeometry() {
        Subject subject = getOrCreateSubject("Geometry");
        for (GeometryOperation op : GeometryOperation.values()) {
            getOrCreateSubSubject(op.getSubSubjectName(), subject);
        }
    }

    // ── Polynomial ───────────────────────────────────────────────────────

    private void seedPolynomial() {
        Subject subject = getOrCreateSubject("Polynomial");
        for (PolynomialOperation op : PolynomialOperation.values()) {
            getOrCreateSubSubject(op.getSubSubjectName(), subject);
        }
    }

    // ── AI ───────────────────────────────────────────────────────────────

    private void seedAi() {
        Subject subject = getOrCreateSubject("AI");
        getOrCreateSubSubject("ai", subject);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Subject getOrCreateSubject(String name) {
        return subjectRepository.findByName(name)
                .orElseGet(() -> {
                    log.info("Creating subject '{}'", name);
                    return subjectRepository.save(new Subject(name));
                });
    }

    private SubSubject getOrCreateSubSubject(String name, Subject subject) {
        SubSubject existing = subSubjectRepository.findByNameAndSubject_Name(name, subject.getName());
        if (existing != null) return existing;
        log.info("Creating sub-subject '{}' under '{}'", name, subject.getName());
        return subSubjectRepository.save(new SubSubject(name, subject));
    }

    private void saveTemplate(SubSubject subSubject, String expression, int difficulty) {
        QuestionTemplate template = new QuestionTemplate(subSubject, expression);
        template.setDifficultyLevel(difficulty);
        templateRepository.save(template);
    }
}
