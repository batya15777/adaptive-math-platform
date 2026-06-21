package com.adaptive.server.service;

import com.adaptive.server.DTOs.AiQuestionRequest;
import com.adaptive.server.DTOs.AiQuestionResponse;
import com.adaptive.server.entity.GeneratedQuestion;
import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.enums.QuestionStatus;
import com.adaptive.server.repository.GeneratedQuestionRepository;
import com.adaptive.server.repository.QuestionArchiveRepository;
import com.adaptive.server.service.QuestionsGenerators.ClusterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calls the Python AI microservice to generate a themed question and maps the response
 * into a {@link Question} attached to the target sub-subject.
 *
 * Questions are stored in {@code generated_questions} as reusable templates (using the
 * NAME placeholder). Before calling the microservice, the service checks the cache by
 * (sub_subject_id, language, difficulty_level, multiple_choice) and filters out questions
 * the student has already seen. Only when every cached variant is exhausted for this student
 * does it call the Python service to generate a fresh question.
 *
 * Cluster-aware: when a {@link ClusterContext} is supplied, the student's cohort weakness
 * and mastery are forwarded to the LLM (as a {@code learning_profile}) so the generated
 * question gently targets that weakness.
 *
 * If the microservice is unavailable the call throws a RuntimeException with a clear
 * message; the caller ({@code LevelManagerService}) catches it and falls back to the
 * code-based generator.
 */
@Service
public class AiQuestionService {

    private static final Logger log = LoggerFactory.getLogger(AiQuestionService.class);
    private static final String ENDPOINT = "/generate-question";

    private final RestTemplate restTemplate;
    private final GeneratedQuestionRepository generatedQuestionRepository;
    private final QuestionArchiveRepository questionArchiveRepository;

    @Value("${ai.microservice.url:http://localhost:8000}")
    private String microserviceUrl;

    @Value("${questions.ai.api-key:}")
    private String aiApiKey;

    public AiQuestionService(RestTemplate restTemplate,
                             GeneratedQuestionRepository generatedQuestionRepository,
                             QuestionArchiveRepository questionArchiveRepository) {
        this.restTemplate = restTemplate;
        this.generatedQuestionRepository = generatedQuestionRepository;
        this.questionArchiveRepository = questionArchiveRepository;
    }

    /**
     * @param subSubject     the sub-subject the question belongs to
     * @param aiTopic        explicit topic description for the LLM; falls back to {@code subSubject.getName()} when null
     * @param theme          narrative theme (e.g. "space", "pirates")
     * @param difficulty     1–10
     * @param language       "en", "he", or "ru"
     * @param multipleChoice when true the response includes 4 answer options
     * @param cluster        the student's ML cohort; pass {@link ClusterContext#neutral()} for none
     * @param userId         used to filter out questions this student has already seen
     */
    public Question generateQuestion(SubSubject subSubject, String aiTopic, String theme, int difficulty,
                                     String language, boolean multipleChoice,
                                     ClusterContext cluster, Long userId) {

        // 1. Fetch what this student has already seen for this sub-subject
        Set<String> seenExpressions = questionArchiveRepository
                .findSeenExpressionsByUserAndSubSubject(userId, subSubject.getId());

        // 2. Cache lookup — all suitable cached questions for this key, excluding seen ones
        List<GeneratedQuestion> available = generatedQuestionRepository
                .findBySubSubjectIdAndLanguageAndDifficultyLevelAndMultipleChoice(
                        subSubject.getId(), language, difficulty, multipleChoice)
                .stream()
                .filter(gq -> !seenExpressions.contains(gq.getExpression()))
                .collect(Collectors.toList());

        // 3. Keep one spare ready: top up until at least 2 suitable unseen questions
        //    exist (one to serve now, one spare) so the next request is instant.
        //    0 unseen → generate 2; 1 unseen → generate 1.
        int stopper = 3;
        while (available.size() < 2 && stopper > 0) {
            stopper--;
            GeneratedQuestion generated = callMicroserviceAndSave(
                    subSubject, aiTopic, theme, difficulty, language, multipleChoice, cluster);
            available.add(generated);
        }

        // 4. Serve one of the available unseen questions
        GeneratedQuestion chosen = available.get(new Random().nextInt(available.size()));
        log.debug("Serving generated_question id={} for user={} ({} unseen available, subSubject={}, lang={}, diff={}, mc={})",
                chosen.getId(), userId, available.size(), subSubject.getId(), language, difficulty, multipleChoice);
        return toQuestion(chosen, subSubject, language);
    }

    /** Calls the Python microservice for a fresh question and persists it to the cache pool. */
    private GeneratedQuestion callMicroserviceAndSave(SubSubject subSubject, String aiTopic, String theme,
                                                      int difficulty, String language, boolean multipleChoice,
                                                      ClusterContext cluster) {
        String topic = (aiTopic != null && !aiTopic.isBlank()) ? aiTopic : subSubject.getName();
        AiQuestionRequest request = new AiQuestionRequest(
                topic, theme, difficulty, language, multipleChoice);
        applyClusterContext(request, cluster);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (aiApiKey != null && !aiApiKey.isBlank()) {
            headers.set("X-API-Key", aiApiKey);
        }

        AiQuestionResponse response;
        try {
            response = restTemplate.postForObject(
                    microserviceUrl + ENDPOINT,
                    new HttpEntity<>(request, headers),
                    AiQuestionResponse.class);
        } catch (RestClientException e) {
            throw new RuntimeException(
                    "AI microservice is unavailable at " + microserviceUrl + ". " +
                    "Start it with: uv run uvicorn main:app --reload", e);
        }

        if (response == null) {
            throw new RuntimeException("AI microservice returned an empty response.");
        }

        GeneratedQuestion toSave = new GeneratedQuestion(
                subSubject,
                response.getQuestionText(),
                response.getCorrectAnswer(),
                response.getStepByStepSolution(),
                response.getOptions(),
                language,
                response.getDifficultyLevel(),
                multipleChoice);
        generatedQuestionRepository.save(toSave);
        log.debug("Saved new generated question to cache (subSubject={}, diff={}, mc={})",
                subSubject.getId(), difficulty, multipleChoice);
        return toSave;
    }

    private Question toQuestion(GeneratedQuestion gq, SubSubject subSubject, String language) {
        return new Question(
                subSubject,
                gq.getExpression(),
                gq.getCorrectAnswer(),
                new ArrayList<>(gq.getSolution()),
                new ArrayList<>(gq.getOptions()),
                language,
                gq.getDifficultyLevel(),
                QuestionStatus.CURRENT);
    }

    /** Attaches the cluster-derived learning profile to the request (no-op when unassigned). */
    private void applyClusterContext(AiQuestionRequest request, ClusterContext cluster) {
        if (cluster == null || !cluster.isAssigned()) {
            return;
        }
        String focusSkill = humanizeFocus(cluster.getFocusErrorPattern());
        String mastery = masteryFromBias(cluster.getDifficultyBias());
        request.setLearningProfile(
                new AiQuestionRequest.LearningProfile(cluster.getLabel(), focusSkill, mastery));
        log.debug("AI request enriched with cluster {} (focus={}, mastery={})",
                cluster.getClusterId(), focusSkill, mastery);
    }

    private String masteryFromBias(int bias) {
        if (bias > 0) return "strong";
        if (bias < 0) return "struggling";
        return "developing";
    }

    /** Turns a raw error-pattern label into a human phrase the LLM can act on. */
    private String humanizeFocus(String errorPattern) {
        if (errorPattern == null) {
            return null;
        }
        switch (errorPattern) {
            case "CONFUSED_SUB_WITH_ADD":   return "subtraction (the student confuses it with addition)";
            case "MINOR_CALCULATION_ERROR": return "careful, accurate calculation";
            case "EMPTY_ANSWER":            return "confidence to attempt every question";
            case "INVALID_FORMAT_ERROR":    return "writing answers in the correct numeric format";
            default:
                String base = errorPattern.startsWith("GENERAL_ERROR_")
                        ? errorPattern.substring("GENERAL_ERROR_".length())
                        : errorPattern;
                return base.toLowerCase().replace('_', ' ');
        }
    }
}
