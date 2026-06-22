package com.adaptive.server.utils;

import com.adaptive.server.entity.ExerciseAttempt;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.Subject;
import com.adaptive.server.entity.User;
import com.adaptive.server.entity.enums.CalculationOperation;
import com.adaptive.server.repository.ExerciseAttemptRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import com.adaptive.server.repository.SubjectRepository;
import com.adaptive.server.repository.UserRepository;
import com.adaptive.server.service.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.adaptive.server.entity.enums.CalculationOperation.ADD;
import static com.adaptive.server.entity.enums.CalculationOperation.DIV;
import static com.adaptive.server.entity.enums.CalculationOperation.MULT;
import static com.adaptive.server.entity.enums.CalculationOperation.SUB;

/**
 * Seeds dummy STUDENT accounts with <b>deliberately distinct</b> attempt histories so the
 * K-Means clustering produces several, clearly-separated cohorts (instead of one degenerate
 * group when there is only a single real student).
 *
 * <p>The four archetypes are engineered against the clustering service's feature set
 * (accuracy, timing, the {@code question_type} of wrong answers, and {@code error_pattern}):</p>
 * <ul>
 *   <li><b>High performers</b> — high accuracy, hard questions, fast, almost no errors.</li>
 *   <li><b>Division strugglers</b> — fine on +/−, but fail division ({@code GENERAL_ERROR_DIV},
 *       wrong-topic fingerprint dominated by DIV).</li>
 *   <li><b>Format/syntax errors</b> — frequently type non-numeric answers
 *       ({@code INVALID_FORMAT_ERROR}; high non-numeric-answer rate).</li>
 *   <li><b>Developing</b> — middling accuracy, minor slips + a few blank answers
 *       ({@code MINOR_CALCULATION_ERROR} / {@code EMPTY_ANSWER}), slower.</li>
 * </ul>
 *
 * <p>Disabled by default — enable with {@code app.seed-dummy-students=true} (env
 * {@code SEED_DUMMY_STUDENTS=true}). Idempotent: each student is skipped if its email already
 * exists, so re-running never duplicates data. Generation is seeded ({@link Random}) for
 * reproducible histories. All accounts share the password printed in the startup log.</p>
 */
@Component
@ConditionalOnProperty(name = "app.seed-dummy-students", havingValue = "true")
public class DummyStudentSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DummyStudentSeeder.class);

    /** Same for every dummy account — valid per the login rules (2–10 chars). */
    private static final String PASSWORD = "123456";

    /** Non-numeric answers used to trigger INVALID_FORMAT_ERROR for the "format" archetype. */
    private static final String[] NON_NUMERIC = {"ten", "12x", "??", "abc", "#", "3,5"};

    /** {email, full name, archetype, human label}. 8 students = 2 per archetype. */
    private static final String[][] SPECS = {
            {"student1@test.com", "Ava Bright",  "HIGH",   "High performer (fast, ~88% accuracy)"},
            {"student2@test.com", "Noah Swift",  "HIGH",   "High performer (fast, ~88% accuracy)"},
            {"student3@test.com", "Mia Lopez",   "DIV",    "Division struggler (~50%, fails DIV)"},
            {"student4@test.com", "Omar Khan",   "DIV",    "Division struggler (~50%, fails DIV)"},
            {"student5@test.com", "Lily Chen",   "FORMAT", "Format/syntax errors (~25%, non-numeric)"},
            {"student6@test.com", "Ethan Ross",  "FORMAT", "Format/syntax errors (~25%, non-numeric)"},
            {"student7@test.com", "Sara Cohen",  "DEV",    "Developing (~56%, minor slips + blanks)"},
            {"student8@test.com", "Jack Miller", "DEV",    "Developing (~56%, minor slips + blanks)"},
    };

    private enum ErrorKind { MINOR, GENERAL, INVALID_FORMAT, EMPTY }

    private final UserRepository userRepository;
    private final ExerciseAttemptRepository attemptRepository;
    private final SubjectRepository subjectRepository;
    private final SubSubjectRepository subSubjectRepository;
    private final PasswordService passwordService;

    /** Resolved once per run: the Calculation operation → its sub-subject FK row. */
    private Map<CalculationOperation, SubSubject> subByOp;

    public DummyStudentSeeder(UserRepository userRepository,
                              ExerciseAttemptRepository attemptRepository,
                              SubjectRepository subjectRepository,
                              SubSubjectRepository subSubjectRepository,
                              PasswordService passwordService) {
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
        this.subjectRepository = subjectRepository;
        this.subSubjectRepository = subSubjectRepository;
        this.passwordService = passwordService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        this.subByOp = resolveCalcSubSubjects();
        // All histories sit in the last day so they also count as "active learners (7d)".
        LocalDateTime base = LocalDateTime.now().minusDays(1);

        List<String[]> created = new ArrayList<>();
        for (int i = 0; i < SPECS.length; i++) {
            String email = SPECS[i][0], fullName = SPECS[i][1], archetype = SPECS[i][2], label = SPECS[i][3];
            if (userRepository.existsByEmail(email)) {
                log.info("Dummy student '{}' already exists — skipping.", email);
                continue;
            }
            User student = createStudent(fullName, email, 10 + (i % 5), (i % 2 == 0) ? "female" : "male");

            Random rnd = new Random(1000L + i);            // reproducible per student
            long[] cursor = { i * 900L };                  // stagger students apart in time
            List<ExerciseAttempt> attempts = buildAttempts(archetype, student, base, rnd, cursor);
            attemptRepository.saveAll(attempts);

            long correct = attempts.stream().filter(ExerciseAttempt::getIsCorrect).count();
            student.setTotalStars((int) correct * 10);     // a little leaderboard flavour
            userRepository.save(student);

            created.add(new String[]{ email, label, attempts.size() + " attempts, ~" + pct(correct, attempts.size()) + "% correct" });
        }
        logResult(created);
    }

    // ── attempt generators (one per archetype) ────────────────────────────────

    private List<ExerciseAttempt> buildAttempts(String archetype, User u, LocalDateTime base, Random r, long[] cursor) {
        List<ExerciseAttempt> a = new ArrayList<>();
        switch (archetype) {
            case "HIGH": // fast (≈5s gaps), hard (diff 2–3), high accuracy, rare minor slips
                addBlock(a, u, ADD,  4, 1.00, ErrorKind.MINOR, 2, 3, 5, r, cursor, base);
                addBlock(a, u, SUB,  4, 1.00, ErrorKind.MINOR, 2, 3, 5, r, cursor, base);
                addBlock(a, u, MULT, 4, 0.75, ErrorKind.MINOR, 2, 3, 5, r, cursor, base);
                addBlock(a, u, DIV,  4, 0.75, ErrorKind.MINOR, 2, 3, 5, r, cursor, base);
                break;
            case "DIV": // good on +/−, fails division → DIV dominates the wrong-topic fingerprint
                addBlock(a, u, ADD, 5, 0.80, ErrorKind.MINOR,   1, 2, 25, r, cursor, base);
                addBlock(a, u, SUB, 5, 0.80, ErrorKind.MINOR,   1, 2, 25, r, cursor, base);
                addBlock(a, u, DIV, 8, 0.125, ErrorKind.GENERAL, 1, 2, 25, r, cursor, base);
                break;
            case "FORMAT": // frequently non-numeric answers → INVALID_FORMAT_ERROR, high non-numeric rate
                addBlock(a, u, ADD,  8, 0.25, ErrorKind.INVALID_FORMAT, 1, 2, 15, r, cursor, base);
                addBlock(a, u, MULT, 8, 0.25, ErrorKind.INVALID_FORMAT, 1, 2, 15, r, cursor, base);
                break;
            case "DEV": // slow (≈45s), middling accuracy, minor slips + a few blank answers
                addBlock(a, u, ADD,  4, 0.50, ErrorKind.MINOR, 1, 2, 45, r, cursor, base);
                addBlock(a, u, SUB,  4, 0.50, ErrorKind.EMPTY, 1, 2, 45, r, cursor, base);
                addBlock(a, u, MULT, 4, 0.75, ErrorKind.MINOR, 1, 2, 45, r, cursor, base);
                addBlock(a, u, DIV,  4, 0.50, ErrorKind.MINOR, 1, 2, 45, r, cursor, base);
                break;
            default:
                log.warn("Unknown archetype '{}' — no attempts generated.", archetype);
        }
        return a;
    }

    /**
     * Appends {@code count} attempts for one operation. The first {@code round(count*accuracy)}
     * are correct (numeric answer, no error); the rest are wrong, shaped by {@code wrongKind}.
     */
    private void addBlock(List<ExerciseAttempt> out, User u, CalculationOperation op, int count,
                          double accuracy, ErrorKind wrongKind, int diffLo, int diffHi,
                          int gapSec, Random r, long[] cursor, LocalDateTime base) {
        SubSubject sub = subByOp.get(op);
        String questionType = op.name();                    // "ADD" / "SUB" / "MULT" / "DIV"
        int correctCount = (int) Math.round(count * accuracy);

        for (int i = 0; i < count; i++) {
            boolean correct = i < correctCount;
            int difficulty = diffLo + (diffHi > diffLo ? r.nextInt(diffHi - diffLo + 1) : 0);

            String userAnswer;
            String errorPattern;
            if (correct) {
                userAnswer = String.valueOf(1 + r.nextInt(20));
                errorPattern = null;
            } else {
                switch (wrongKind) {
                    case INVALID_FORMAT:
                        userAnswer = NON_NUMERIC[r.nextInt(NON_NUMERIC.length)];
                        errorPattern = "INVALID_FORMAT_ERROR";
                        break;
                    case EMPTY:
                        userAnswer = "";
                        errorPattern = "EMPTY_ANSWER";
                        break;
                    case GENERAL:
                        userAnswer = String.valueOf(50 + r.nextInt(50));   // clearly wrong, numeric
                        errorPattern = "GENERAL_ERROR_" + questionType;     // e.g. GENERAL_ERROR_DIV
                        break;
                    case MINOR:
                    default:
                        userAnswer = String.valueOf(1 + r.nextInt(20));     // numeric, "off by a bit"
                        errorPattern = "MINOR_CALCULATION_ERROR";
                        break;
                }
            }

            cursor[0] += gapSec + r.nextInt(3);             // archetype pace + tiny jitter
            LocalDateTime answeredAt = base.plusSeconds(cursor[0]);
            out.add(new ExerciseAttempt(u, sub, null, correct, difficulty, questionType, userAnswer, errorPattern, answeredAt));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User createStudent(String fullName, String email, int age, String gender) {
        User u = new User(fullName, passwordService.hash(PASSWORD), email, age, gender, LocalDateTime.now());
        u.setRole("STUDENT");
        u.setAccountStatus("ACTIVE");
        return userRepository.save(u);
    }

    /** Get-or-create the Calculation subject + its add/sub/mult/div sub-subjects (self-sufficient). */
    private Map<CalculationOperation, SubSubject> resolveCalcSubSubjects() {
        Subject calculation = subjectRepository.findByName("Calculation")
                .orElseGet(() -> subjectRepository.save(new Subject("Calculation")));

        Map<CalculationOperation, SubSubject> map = new EnumMap<>(CalculationOperation.class);
        for (CalculationOperation op : new CalculationOperation[]{ ADD, SUB, MULT, DIV }) {
            SubSubject ss = subSubjectRepository.findByNameAndSubject_Name(op.getSubSubjectName(), "Calculation");
            if (ss == null) {
                ss = subSubjectRepository.save(new SubSubject(op.getSubSubjectName(), calculation));
            }
            map.put(op, ss);
        }
        return map;
    }

    private static int pct(long part, long whole) {
        return whole <= 0 ? 0 : (int) Math.round(100.0 * part / whole);
    }

    private void logResult(List<String[]> created) {
        if (created.isEmpty()) {
            log.info("Dummy students already present — nothing seeded.");
            return;
        }
        StringBuilder sb = new StringBuilder("\n");
        sb.append("============ Dummy students seeded (password for all: ").append(PASSWORD).append(") ============\n");
        for (String[] row : created) {
            sb.append(String.format("  %-22s | %-42s | %s%n", row[0], row[1], row[2]));
        }
        sb.append("=======================================================================================\n");
        sb.append("  Next: log in as an admin → /admin/ml-groups → \"Run clustering\" to see the cohorts.\n");
        log.warn(sb.toString());
    }
}
