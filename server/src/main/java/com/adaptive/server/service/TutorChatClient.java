package com.adaptive.server.service;

import com.adaptive.server.DTOs.TutorMicroserviceRequest;
import com.adaptive.server.DTOs.TutorMicroserviceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class TutorChatClient {

    private static final String ENDPOINT = "/chat";

    private final RestTemplate restTemplate;

    @Value("${ai.tutor.url:http://127.0.0.1:8001}")
    private String tutorUrl;

    public TutorChatClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TutorMicroserviceResponse askTutor(TutorMicroserviceRequest request) {
        try {
            TutorMicroserviceResponse response = restTemplate.postForObject(
                    tutorUrl + ENDPOINT,
                    request,
                    TutorMicroserviceResponse.class
            );

            if (response == null) {
                throw new RuntimeException("AI tutor microservice returned an empty response.");
            }

            return response;
        } catch (RestClientException e) {
            throw new RuntimeException(
                    "AI tutor microservice is unavailable at " + tutorUrl + ". " +
                    "Start it with: uv run uvicorn main:app --reload --port 8001", e);
        }
    }
}
