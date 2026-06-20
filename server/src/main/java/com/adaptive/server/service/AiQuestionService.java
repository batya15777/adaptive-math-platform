package com.adaptive.server.service;

import com.adaptive.server.DTOs.AiQuestionRequest;
import com.adaptive.server.DTOs.AiQuestionResponse;
import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.enums.QuestionStatus;
import com.adaptive.server.service.QuestionsGenerators.ClusterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Calls the Python AI microservice to generate a themed, personalised question and
 * maps the response into a {@link Question} attached to the target sub-subject.
 *
 * Cluster-aware: when a {@link ClusterContext} is supplied, the student's cohort
 * weakness and mastery are forwarded to the LLM (as a {@code learning_profile}) so
 * the generated question gently targets that weakness.
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

    @Value("${ai.microservice.url:http://localhost:8000}")
    private String microserviceUrl;

    public AiQuestionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * @param subSubject     the sub-subject the question belongs to (its name becomes the AI topic)
     * @param theme          narrative theme (e.g. "space", "pirates")
     * @param difficulty     1–10
     * @param userName       student's name for personalisation
     * @param userAge        student's age (4–18)
     * @param language       "en", "he", or "ru"
     * @param multipleChoice when true the response includes 4 answer options
     * @param cluster        the student's ML cohort; pass {@link ClusterContext#neutral()} for none
     */
    public Question generateQuestion(SubSubject subSubject, String theme, int difficulty,
                                     String userName, int userAge, String language,
                                     boolean multipleChoice, ClusterContext cluster) {

        AiQuestionRequest request = new AiQuestionRequest(
                subSubject.getName(), theme, difficulty, userName, userAge, language, multipleChoice);
        applyClusterContext(request, cluster);

        AiQuestionResponse response;
        try {
            response = restTemplate.postForObject(
                    microserviceUrl + ENDPOINT, request, AiQuestionResponse.class);
        } catch (RestClientException e) {
            throw new RuntimeException(
                    "AI microservice is unavailable at " + microserviceUrl + ". " +
                    "Start it with: uv run uvicorn main:app --reload", e);
        }

        if (response == null) {
            throw new RuntimeException("AI microservice returned an empty response.");
        }

        return new Question(
                subSubject,
                response.getQuestionText(),
                response.getCorrectAnswer(),
                response.getStepByStepSolution(),
                response.getOptions(),
                language,
                response.getDifficultyLevel(),
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
