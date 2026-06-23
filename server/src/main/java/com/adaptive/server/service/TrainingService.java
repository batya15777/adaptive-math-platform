package com.adaptive.server.service;

import com.adaptive.server.entity.StudentProgress;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.repository.ExerciseAttemptRepository;
import com.adaptive.server.repository.StudentProgressRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import com.adaptive.server.repository.SubjectRepository;
import com.adaptive.server.repository.TopicRepository;
import com.adaptive.server.responses.SubSubjectProgressResponse;
import com.adaptive.server.responses.SubjectResponse;
import com.adaptive.server.responses.TopicResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Read-only assembly for the Math Training selection page:
 * topics → subjects → sub-subjects (with the student's progress on each).
 */
@Service
public class TrainingService {

    // Difficulty scale upper bound (1–10), matching LevelManagerService + the generators.
    private static final int MAX_DIFFICULTY_LEVEL = 10;
    // Defaults used when the student hasn't started a sub-subject yet.
    private static final int DEFAULT_LEVEL = 1;

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final SubSubjectRepository subSubjectRepository;
    private final StudentProgressRepository progressRepository;
    private final ExerciseAttemptRepository attemptRepository;
    private final ClusterContextService clusterContextService;

    public TrainingService(TopicRepository topicRepository,
                           SubjectRepository subjectRepository,
                           SubSubjectRepository subSubjectRepository,
                           StudentProgressRepository progressRepository,
                           ExerciseAttemptRepository attemptRepository,
                           ClusterContextService clusterContextService) {
        this.topicRepository       = topicRepository;
        this.subjectRepository     = subjectRepository;
        this.subSubjectRepository  = subSubjectRepository;
        this.progressRepository    = progressRepository;
        this.attemptRepository     = attemptRepository;
        this.clusterContextService = clusterContextService;
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> getTopics() {
        // De-duplicate by name in case the DB has multiple rows with the same topic name
        // (e.g. caused by the seeder running on two separate server starts).
        // toMap keeps the first occurrence; LinkedHashMap preserves the sorted order.
        return topicRepository.findByActiveTrueOrderByNameAsc().stream()
                .collect(Collectors.toMap(
                        t -> t.getName(),
                        t -> t,
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new
                ))
                .values().stream()
                .map(TopicResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> getSubjects(Long topicId) {
        return subjectRepository.findByTopicIdAndTopicActiveTrueAndActiveTrueOrderByNameAsc(topicId).stream()
                .map(SubjectResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubSubjectProgressResponse> getSubSubjects(Long userId, Long subjectId) {
        // The cluster difficulty bias is per-student (not per sub-subject), so resolve it once.
        int clusterBias = clusterContextService.forUser(userId).getDifficultyBias();
        List<SubSubject> subSubjects = subSubjectRepository
                .findActiveForStudentBySubjectId(subjectId);
        return subSubjects.stream()
                .map(sub -> toProgressResponse(userId, sub, clusterBias))
                .collect(Collectors.toList());
    }

    private SubSubjectProgressResponse toProgressResponse(Long userId, SubSubject sub, int clusterBias) {
        Optional<StudentProgress> progress =
                progressRepository.findByUserIdAndSubSubjectId(userId, sub.getId());

        int currentLevel = progress.map(StudentProgress::getCurrentLevel).orElse(DEFAULT_LEVEL);
        boolean inSubLevel = progress.map(StudentProgress::getInSubLevel).orElse(false);

        long questionsSolved =
                attemptRepository.countByUserIdAndSubSubjectIdAndIsCorrectTrue(userId, sub.getId());

        // The difficulty the NEXT question will actually be served at — mirrors
        // LevelManagerService.getNextQuestion: the base difficulty (sub-level forces the easiest)
        // plus the student's cluster difficulty bias, clamped to the 1–10 scale. Computing it this
        // way (rather than from past attempts) means the card matches the in-game "רמת קושי N"
        // even for a sub-subject the student hasn't answered yet.
        int base = inSubLevel ? 1 : Math.min(currentLevel, MAX_DIFFICULTY_LEVEL);
        int difficultyLevel = Math.max(1, Math.min(base + clusterBias, MAX_DIFFICULTY_LEVEL));

        return new SubSubjectProgressResponse(
                sub.getId(), sub.getName(), currentLevel, questionsSolved, difficultyLevel);
    }
}
