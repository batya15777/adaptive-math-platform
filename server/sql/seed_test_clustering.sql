-- ============================================================================
--  Test-data seed for the ML clustering endpoint  (MySQL / adaptive_db)
--  POST /ml/clustering/run  should now return two clean clusters.
--
--  Safe & re-runnable:
--   * users are inserted by EMAIL (INSERT IGNORE) so real rows are never touched
--   * the 4 test users' attempts are wiped & re-inserted each run (no duplicates)
--   * if no sub_subjects exist yet, a minimal Calculation→mult chain is bootstrapped
--
--  Run the WHOLE script in one go so the @variables persist (see notes below).
-- ============================================================================

-- 0) Make sure at least one sub_subject exists (FK target for exercise_attempts).
--    These three only run when the sub_subjects table is completely empty.
INSERT INTO topics (name)
SELECT 'Calculation' WHERE NOT EXISTS (SELECT 1 FROM sub_subjects);

INSERT INTO subjects (name, topic_id)
SELECT 'Calculation', (SELECT MAX(id) FROM topics)
WHERE NOT EXISTS (SELECT 1 FROM sub_subjects);

INSERT INTO sub_subjects (name, subject_id)
SELECT 'mult', (SELECT MAX(id) FROM subjects)
WHERE NOT EXISTS (SELECT 1 FROM sub_subjects);

-- Resolve a valid sub_subject id for each group (prefer add/mult, fall back to any).
SET @ss_group1 := COALESCE(
        (SELECT id FROM sub_subjects WHERE name = 'add'  ORDER BY id LIMIT 1),
        (SELECT MIN(id) FROM sub_subjects));
SET @ss_group2 := COALESCE(
        (SELECT id FROM sub_subjects WHERE name = 'mult' ORDER BY id LIMIT 1),
        (SELECT MIN(id) FROM sub_subjects));

-- 1) Four mock users (idempotent: skipped if the email already exists).
INSERT IGNORE INTO users (full_name, password_hash, email, age, gender, created_at, total_stars, role)
VALUES
    ('Test Student A', '$noop$test', 'testA@adaptive.dev', 10, 'F', NOW(), 0, 'STUDENT'),
    ('Test Student B', '$noop$test', 'testB@adaptive.dev', 11, 'M', NOW(), 0, 'STUDENT'),
    ('Test Student C', '$noop$test', 'testC@adaptive.dev', 10, 'M', NOW(), 0, 'STUDENT'),
    ('Test Student D', '$noop$test', 'testD@adaptive.dev', 12, 'F', NOW(), 0, 'STUDENT');

-- Resolve their real (auto-increment) ids by the stable email key.
SET @uA := (SELECT id FROM users WHERE email = 'testA@adaptive.dev');
SET @uB := (SELECT id FROM users WHERE email = 'testB@adaptive.dev');
SET @uC := (SELECT id FROM users WHERE email = 'testC@adaptive.dev');
SET @uD := (SELECT id FROM users WHERE email = 'testD@adaptive.dev');

-- 2) Clear any previous attempts for ONLY these 4 test users, then re-insert.
DELETE FROM exercise_attempts WHERE user_id IN (@uA, @uB, @uC, @uD);

INSERT INTO exercise_attempts
    (user_id, sub_subject_id, question_id, is_correct, difficulty_level, question_type, answered_at, user_answer, error_pattern)
VALUES
    -- ── Group 1 (A & B): high success — is_correct = 1, error_pattern = NULL ──
    (@uA, @ss_group1, NULL, 1, 2, 'SIMPLE_ADDITION', NOW() - INTERVAL 30 MINUTE, '15', NULL),
    (@uA, @ss_group1, NULL, 1, 2, 'SIMPLE_ADDITION', NOW() - INTERVAL 27 MINUTE, '22', NULL),
    (@uA, @ss_group1, NULL, 1, 3, 'SIMPLE_ADDITION', NOW() - INTERVAL 24 MINUTE, '8',  NULL),
    (@uB, @ss_group1, NULL, 1, 2, 'SIMPLE_ADDITION', NOW() - INTERVAL 30 MINUTE, '12', NULL),
    (@uB, @ss_group1, NULL, 1, 3, 'SIMPLE_ADDITION', NOW() - INTERVAL 26 MINUTE, '19', NULL),
    (@uB, @ss_group1, NULL, 1, 2, 'SIMPLE_ADDITION', NOW() - INTERVAL 22 MINUTE, '7',  NULL),

    -- ── Group 2 (C & D): consistent failure — is_correct = 0, error_pattern = 'GENERAL_ERROR_MULT' ──
    (@uC, @ss_group2, NULL, 0, 3, 'MULTIPLICATION', NOW() - INTERVAL 30 MINUTE, '99', 'GENERAL_ERROR_MULT'),
    (@uC, @ss_group2, NULL, 0, 3, 'MULTIPLICATION', NOW() - INTERVAL 27 MINUTE, '40', 'GENERAL_ERROR_MULT'),
    (@uC, @ss_group2, NULL, 0, 2, 'MULTIPLICATION', NOW() - INTERVAL 24 MINUTE, '12', 'GENERAL_ERROR_MULT'),
    (@uD, @ss_group2, NULL, 0, 3, 'MULTIPLICATION', NOW() - INTERVAL 30 MINUTE, '50', 'GENERAL_ERROR_MULT'),
    (@uD, @ss_group2, NULL, 0, 2, 'MULTIPLICATION', NOW() - INTERVAL 26 MINUTE, '33', 'GENERAL_ERROR_MULT'),
    (@uD, @ss_group2, NULL, 0, 3, 'MULTIPLICATION', NOW() - INTERVAL 22 MINUTE, '7',  'GENERAL_ERROR_MULT');

-- 3) Sanity check — should print 4 rows: A & B with 100% success, C & D with 0%.
SELECT u.email,
       COUNT(*)                                              AS attempts,
       SUM(ea.is_correct)                                    AS correct,
       ROUND(100 * SUM(ea.is_correct) / COUNT(*))            AS success_pct,
       MAX(ea.error_pattern)                                 AS sample_error
FROM exercise_attempts ea
JOIN users u ON u.id = ea.user_id
WHERE u.email IN ('testA@adaptive.dev','testB@adaptive.dev','testC@adaptive.dev','testD@adaptive.dev')
GROUP BY u.email
ORDER BY u.email;
