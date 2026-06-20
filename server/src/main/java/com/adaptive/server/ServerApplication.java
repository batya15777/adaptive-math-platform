package com.adaptive.server;

import com.adaptive.server.entity.QuestionTemplate;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.Subject;
import com.adaptive.server.entity.Topic;
import com.adaptive.server.entity.User;
import com.adaptive.server.repository.QuestionTemplateRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import com.adaptive.server.repository.SubjectRepository;
import com.adaptive.server.repository.TopicRepository;
import com.adaptive.server.repository.UserRepository;
import com.adaptive.server.utils.GenerateHash;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    @Bean
    CommandLineRunner createMockUser(UserRepository userRepository) {
        return args -> {
            String testEmail = "test@example.com";

            if (!userRepository.existsByEmail(testEmail)) {
                String myUsername = "jon";
                String myPassword = "wow";
                String hash = GenerateHash.hashMd5(myUsername, myPassword);

                User mockUser = new User(
                        myUsername,
                        hash,
                        testEmail,
                        10,
                        "female",
                        LocalDateTime.now()
                );
                userRepository.save(mockUser);
                System.out.println("Mock user created successfully! Login with: " + testEmail + " / " + myPassword);
            }
        };
    }

    /**
     * Seeds the Calculation subject with five sub-subjects (add, sub, mult, div, mixed)
     * and a 3×3 matrix of question templates per sub-subject:
     *
     * <pre>
     *   subSubjectLevel (1=beginner, 2=intermediate, 3=advanced)
     *     × difficultyLevel (1=simple expression, 2=medium, 3=complex)
     * </pre>
     *
     * <p><strong>Design rationale:</strong> {@code subSubjectLevel} on a template corresponds
     * to the student's current progress level (capped at 3 in {@code CalculationGenerator}).
     * Higher levels expose more complex expression structures AND the generator fills X
     * placeholders with larger numbers (since the range formula uses {@code subSubjectLevel}
     * directly).  This gives both structural and numeric difficulty scaling.
     *
     * <p>Duplicate detection uses all three dimensions (expression + difficultyLevel +
     * subSubjectLevel) so that the same expression string can legitimately appear at
     * different student levels with different numeric ranges.
     */
    @Bean
    CommandLineRunner createMockCalculationData(
            TopicRepository topicRepository,
            SubjectRepository subjectRepository,
            SubSubjectRepository subSubjectRepository,
            QuestionTemplateRepository templateRepository) {

        return args -> {
            // Subject.topic is required — ensure a "Math" topic exists and the
            // Calculation subject is linked to it (covers a fresh insert and an
            // older row whose topic_id was never set).
            Topic math = topicRepository.findByName("Math")
                    .orElseGet(() -> topicRepository.save(new Topic("Math")));

            Subject calculation = subjectRepository.findByName("Calculation")
                    .orElse(null);
            if (calculation == null) {
                calculation = new Subject("Calculation");
                calculation.setTopic(math);
                calculation = subjectRepository.save(calculation);
            } else if (calculation.getTopic() == null) {
                calculation.setTopic(math);
                calculation = subjectRepository.save(calculation);
            }

            // ── Template seed matrix ─────────────────────────────────────────
            // Each record: { expression, difficultyLevel, subSubjectLevel }
            // subSubjectLevel 1 = beginner  (student level 1–3  maps here)
            // subSubjectLevel 2 = intermediate (student level 4–6)
            // subSubjectLevel 3 = advanced  (student level 7+)
            seedSubject(calculation, "add", List.of(
                    // beginner: 2-operand then 3 then 4
                    new int[]{1, 1}, "X + X",
                    new int[]{2, 1}, "X + X + X",
                    new int[]{3, 1}, "X + X + X + X",
                    // intermediate: 3 then 4 then 5
                    new int[]{1, 2}, "X + X + X",
                    new int[]{2, 2}, "X + X + X + X",
                    new int[]{3, 2}, "X + X + X + X + X",
                    // advanced: 4 then 5 then 6
                    new int[]{1, 3}, "X + X + X + X",
                    new int[]{2, 3}, "X + X + X + X + X",
                    new int[]{3, 3}, "X + X + X + X + X + X"
            ), subSubjectRepository, templateRepository);

            seedSubject(calculation, "sub", List.of(
                    new int[]{1, 1}, "X - X",
                    new int[]{2, 1}, "X - X - X",
                    new int[]{3, 1}, "X - X - X - X",
                    new int[]{1, 2}, "X - X - X",
                    new int[]{2, 2}, "X - X - X - X",
                    new int[]{3, 2}, "X - X - X - X - X",
                    new int[]{1, 3}, "X - X - X - X",
                    new int[]{2, 3}, "X - X - X - X - X",
                    new int[]{3, 3}, "X - X - X - X - X - X"
            ), subSubjectRepository, templateRepository);

            seedSubject(calculation, "mult", List.of(
                    new int[]{1, 1}, "X * X",
                    new int[]{2, 1}, "X * X * X",
                    new int[]{3, 1}, "X * X * X * X",
                    new int[]{1, 2}, "X * X * X",
                    new int[]{2, 2}, "X * X * X * X",
                    new int[]{3, 2}, "X * X * X * X * X",
                    new int[]{1, 3}, "X * X * X * X",
                    new int[]{2, 3}, "X * X * X * X * X",
                    new int[]{3, 3}, "X * X * X * X * X * X"
            ), subSubjectRepository, templateRepository);

            seedSubject(calculation, "div", List.of(
                    new int[]{1, 1}, "X / X",
                    new int[]{2, 1}, "X / X / X",
                    new int[]{3, 1}, "X / X / X / X",
                    new int[]{1, 2}, "X / X / X",
                    new int[]{2, 2}, "X / X / X / X",
                    new int[]{3, 2}, "X / X / X / X / X",
                    new int[]{1, 3}, "X / X / X / X",
                    new int[]{2, 3}, "X / X / X / X / X",
                    new int[]{3, 3}, "X / X / X / X / X / X"
            ), subSubjectRepository, templateRepository);
        };
    }

    /**
     * Seeds one sub-subject's template matrix. The {@code entries} list alternates
     * between {@code int[]{difficultyLevel, subSubjectLevel}} arrays and expression strings
     * so each pair reads as a self-contained record in the source code.
     *
     * <p>Duplicate detection uses all three dimensions (expression, difficulty, subjectLevel)
     * so the same expression string can exist at different subjectLevel bands without
     * triggering a false duplicate.
     */
    private void seedSubject(Subject subject, String subName,
                              List<Object> entries,
                              SubSubjectRepository subSubjectRepository,
                              QuestionTemplateRepository templateRepository) {

        SubSubject sub = subSubjectRepository.findByNameAndSubject_Name(subName, subject.getName());
        if (sub == null) {
            sub = subSubjectRepository.save(new SubSubject(subName, subject));
            System.out.println("[Seed] Created sub-subject: " + subName);
        }

        // entries alternate: int[]{difficultyLevel, subSubjectLevel}, String expression, ...
        for (int i = 0; i < entries.size(); i += 2) {
            int[] dims       = (int[]) entries.get(i);
            int difficulty   = dims[0];
            int subjectLevel = dims[1];
            String expression = (String) entries.get(i + 1);

            boolean alreadySeeded = !templateRepository
                    .findAllBySubSubjectAndDifficultyLevelAndSubSubjectLevel(
                            sub, difficulty, subjectLevel)
                    .stream()
                    .filter(t -> expression.equals(t.getExpression()))
                    .findAny()
                    .isEmpty();

            if (!alreadySeeded) {
                QuestionTemplate t = new QuestionTemplate(sub, expression);
                t.setDifficultyLevel(difficulty);
                t.setSubSubjectLevel(subjectLevel);
                templateRepository.save(t);
                System.out.printf("[Seed] %-6s | ssl=%d diff=%d | %s%n",
                        subName, subjectLevel, difficulty, expression);
            }
        }
    }
}
