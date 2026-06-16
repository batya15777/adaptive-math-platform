package com.adaptive.server.service;

import com.adaptive.server.DTOs.AiQuestionRequest;
import com.adaptive.server.DTOs.AiQuestionResponse;
import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.enums.QuestionStatus;
import com.adaptive.server.repository.SubSubjectRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Calls the Python AI microservice to generate a context-rich, themed question
 * and maps the response into the standard Question entity used by the rest of the server.
 *
 * The microservice must be running; if it is unavailable the call throws
 * a RuntimeException with a clear message.
 */
@Service
public class AiQuestionService {

    private static final String SUBJECT_NAME     = "AI";
    private static final String SUB_SUBJECT_NAME = "ai";
    private static final String ENDPOINT         = "/generate-question";

    private final RestTemplate restTemplate;
    private final SubSubjectRepository subSubjectRepository;

    @Value("${ai.microservice.url:http://localhost:8000}")
    private String microserviceUrl;

    public AiQuestionService(RestTemplate restTemplate,
                             SubSubjectRepository subSubjectRepository) {
        this.restTemplate         = restTemplate;
        this.subSubjectRepository = subSubjectRepository;
    }

    /**
     * @param topic          Math topic (e.g. "fractions", "geometry")
     * @param theme          Narrative theme (e.g. "pirates", "space")
     * @param difficulty     1–10 (1 = kindergarten, 10 = advanced high-school)
     * @param userName       Student's first name for personalisation
     * @param userAge        Student's age (4–18)
     * @param language       "en", "he", or "ru"
     * @param multipleChoice When true the response includes 4 answer options
     */
    public Question generateQuestion(String topic, String theme, int difficulty,
                                     String userName, int userAge,
                                     String language, boolean multipleChoice) {
        SubSubject subSubject = subSubjectRepository.findByNameAndSubject_Name(SUB_SUBJECT_NAME, SUBJECT_NAME);
        if (subSubject == null) {
            throw new RuntimeException(
                    "SubSubject '" + SUB_SUBJECT_NAME + "' not found under subject '" + SUBJECT_NAME + "'. " +
                    "Make sure the database is seeded (SEED_DATA=true).");
        }

        AiQuestionRequest request = new AiQuestionRequest(
                topic, theme, difficulty, userName, userAge, language, multipleChoice);

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
                QuestionStatus.CURRENT
        );
    }
}
