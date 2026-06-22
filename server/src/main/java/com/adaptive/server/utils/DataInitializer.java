package com.adaptive.server.utils;

import com.adaptive.server.entity.QuestionTemplate;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.Subject;
import com.adaptive.server.entity.Topic;
import com.adaptive.server.entity.enums.CalculationOperation;
import com.adaptive.server.entity.enums.GeometryOperation;
import com.adaptive.server.entity.enums.PolynomialOperation;
import com.adaptive.server.repository.QuestionTemplateRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import com.adaptive.server.repository.SubjectRepository;
import com.adaptive.server.repository.TopicRepository;
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

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final SubSubjectRepository subSubjectRepository;
    private final QuestionTemplateRepository templateRepository;

    public DataInitializer(TopicRepository topicRepository,
                           SubjectRepository subjectRepository,
                           SubSubjectRepository subSubjectRepository,
                           QuestionTemplateRepository templateRepository) {
        this.topicRepository      = topicRepository;
        this.subjectRepository    = subjectRepository;
        this.subSubjectRepository = subSubjectRepository;
        this.templateRepository   = templateRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Seeding initial data...");
        // Respect the Topic → Subject → SubSubject hierarchy: the umbrella topic must
        // exist (and be saved) before any subject, since Subject.topic is a not-null FK.
        Topic mathematics = getOrCreateTopic("Mathematics");
        seedCalculation(mathematics);
        seedGeometry(mathematics);
        seedPolynomial(mathematics);
        seedAi(mathematics);
        log.info("Seeding done.");
    }

    // ── Calculation ──────────────────────────────────────────────────────

    private void seedCalculation(Topic topic) {
        Subject subject = getOrCreateSubject("Calculation", topic);
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
    // Reserved geometry catalogue rows. Question generation is not enabled yet,
    // but keeping the seed data allows the feature to be added without manual DB setup.

    private void seedGeometry(Topic topic) {
        Subject subject = getOrCreateSubject("Geometry", topic);
        for (GeometryOperation op : GeometryOperation.values()) {
            getOrCreateSubSubject(op.getSubSubjectName(), subject);
        }
    }

    // ── Polynomial ───────────────────────────────────────────────────────

    private void seedPolynomial(Topic topic) {
        Subject subject = getOrCreateSubject("Polynomial", topic);
        for (PolynomialOperation op : PolynomialOperation.values()) {
            getOrCreateSubSubject(op.getSubSubjectName(), subject);
        }
    }

    // ── AI ───────────────────────────────────────────────────────────────

    private void seedAi(Topic topic) {
        Subject subject = getOrCreateSubject("AI", topic);
        getOrCreateSubSubject("ai", subject);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Topic getOrCreateTopic(String name) {
        return topicRepository.findByName(name)
                .orElseGet(() -> {
                    log.info("Creating topic '{}'", name);
                    return topicRepository.save(new Topic(name));
                });
    }

    private Subject getOrCreateSubject(String name, Topic topic) {
        return subjectRepository.findByName(name)
                .orElseGet(() -> {
                    log.info("Creating subject '{}' under topic '{}'", name, topic.getName());
                    Subject subject = new Subject(name);
                    subject.setTopic(topic); // satisfy the not-null topic_id FK
                    return subjectRepository.save(subject);
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
