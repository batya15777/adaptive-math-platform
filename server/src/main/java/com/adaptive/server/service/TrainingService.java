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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Read-only assembly for the Math Training selection page:
 * topics → subjects → sub-subjects (with the student's progress on each).
 */
@Service
public class TrainingService {

    // Difficulty band shown to the student is capped here (matches CalculationGenerator).
    private static final int MAX_DIFFICULTY_BAND = 3;
    // Defaults used when the student hasn't started a sub-subject yet.
    private static final int DEFAULT_LEVEL = 1;

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final SubSubjectRepository subSubjectRepository;
    private final StudentProgressRepository progressRepository;
    private final ExerciseAttemptRepository attemptRepository;

    public TrainingService(TopicRepository topicRepository,
                           SubjectRepository subjectRepository,
                           SubSubjectRepository subSubjectRepository,
                           StudentProgressRepository progressRepository,
                           ExerciseAttemptRepository attemptRepository) {
        this.topicRepository      = topicRepository;
        this.subjectRepository    = subjectRepository;
        this.subSubjectRepository = subSubjectRepository;
        this.progressRepository   = progressRepository;
        this.attemptRepository    = attemptRepository;
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> getTopics() {
        return topicRepository.findByActiveTrueOrderByNameAsc().stream()
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
        List<SubSubject> subSubjects = subSubjectRepository.findBySubjectId(subjectId);
        return subSubjects.stream()
                .map(sub -> toProgressResponse(userId, sub))
                .collect(Collectors.toList());
    }

    private SubSubjectProgressResponse toProgressResponse(Long userId, SubSubject sub) {
        Optional<StudentProgress> progress =
                progressRepository.findByUserIdAndSubSubjectId(userId, sub.getId());

        int currentLevel = progress
                .map(StudentProgress::getCurrentLevel)
                .orElse(DEFAULT_LEVEL);

        long questionsSolved =
                attemptRepository.countByUserIdAndSubSubjectIdAndIsCorrectTrue(userId, sub.getId());

        int difficultyLevel = Math.min(currentLevel, MAX_DIFFICULTY_BAND);

        return new SubSubjectProgressResponse(
                sub.getId(), sub.getName(), currentLevel, questionsSolved, difficultyLevel);
    }
}
