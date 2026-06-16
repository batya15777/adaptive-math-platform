package com.adaptive.server;

import com.adaptive.server.entity.QuestionTemplate;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.Subject;
import com.adaptive.server.entity.User;
import com.adaptive.server.repository.QuestionTemplateRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import com.adaptive.server.repository.SubjectRepository;
import com.adaptive.server.repository.UserRepository;
import com.adaptive.server.utils.GenerateHash;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class ServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    @Bean
    CommandLineRunner createMockUser(UserRepository userRepository) {
        return args -> {
            String testEmail = "test@example.com";

            // נבדוק אם המשתמש כבר קיים כדי לא ליצור כפילויות בכל הרצה
            if (!userRepository.existsByEmail(testEmail)) {

                String myUsername = "jon";
                String myPassword = "wow";

                // מייצרים את ה-Hash בדיוק כמו שהמערכת שלך מצפה
                String hash = GenerateHash.hashMd5(myUsername, myPassword);

                // יוצרים את המשתמש
                User mockUser = new User(
                        myUsername,
                        hash,
                        testEmail,
                        10,
                        "female",
                        LocalDateTime.now()
                );

                // שומרים ב-DB
                userRepository.save(mockUser);
                System.out.println("Mock user created successfully! You can now login with: " + testEmail + " / " + myPassword);
            }
        };
    }

    @Bean
    CommandLineRunner createMockCalculationData(
            SubjectRepository subjectRepository,
            SubSubjectRepository subSubjectRepository,
            QuestionTemplateRepository templateRepository) {
        return args -> {

            // ── Subject ──────────────────────────────────────────────────
            Subject calculation = subjectRepository.findByName("Calculation")
                    .orElseGet(() -> subjectRepository.save(new Subject("Calculation")));

            // ── SubSubjects + their templates ────────────────────────────
            // Map: sub-subject name → list of (expression, difficultyLevel)
            Map<String, List<Object[]>> data = Map.of(
                "add", List.of(
                    new Object[]{"X + X",         1},
                    new Object[]{"X + X + X",     2},
                    new Object[]{"X + X + X + X", 3}
                ),
                "sub", List.of(
                    new Object[]{"X - X",         1},
                    new Object[]{"X - X - X",     2},
                    new Object[]{"X - X - X - X", 3}
                ),
                "mult", List.of(
                    new Object[]{"X * X",         1},
                    new Object[]{"X * X * X",     2},
                    new Object[]{"X * X * X * X", 3}
                ),
                "div", List.of(
                    new Object[]{"X / X",         1},
                    new Object[]{"X / X / X",     2},
                    new Object[]{"X / X / X / X", 3}
                ),
                "mixed", List.of(
                    new Object[]{"X + X * X",             1},
                    new Object[]{"X * X - X + X",         2},
                    new Object[]{"X * X + X / X - X",     3}
                )
            );

            for (Map.Entry<String, List<Object[]>> entry : data.entrySet()) {
                String subName = entry.getKey();

                // get or create the sub-subject
                SubSubject sub = subSubjectRepository.findByNameAndSubject_Name(subName, "Calculation");
                if (sub == null) {
                    sub = subSubjectRepository.save(new SubSubject(subName, calculation));
                    System.out.println("Created sub-subject: " + subName);
                }

                // load existing expressions for this sub-subject to detect duplicates
                List<String> existingExpressions = templateRepository
                        .findAllBySubSubject_Name(subName)
                        .stream()
                        .map(QuestionTemplate::getExpression)
                        .toList();

                for (Object[] row : entry.getValue()) {
                    String expression = (String) row[0];
                    int difficulty    = (int)    row[1];

                    if (!existingExpressions.contains(expression)) {
                        QuestionTemplate t = new QuestionTemplate(sub, expression);
                        t.setDifficultyLevel(difficulty);
                        templateRepository.save(t);
                        System.out.println("Created template [" + subName + " | diff=" + difficulty + "]: " + expression);
                    }
                }
            }
        };
    }
}

