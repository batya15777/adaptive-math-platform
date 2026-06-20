package com.adaptive.server.service;

import com.adaptive.server.DTOs.ConversationMessage;
import com.adaptive.server.DTOs.TutorChatHistoryMessage;
import com.adaptive.server.DTOs.TutorChatRequest;
import com.adaptive.server.DTOs.TutorChatResponse;
import com.adaptive.server.DTOs.TutorConversationSummary;
import com.adaptive.server.DTOs.TutorMicroserviceRequest;
import com.adaptive.server.DTOs.TutorMicroserviceResponse;
import com.adaptive.server.DTOs.TutorQuestionContext;
import com.adaptive.server.DTOs.TutorStudentContext;
import com.adaptive.server.entity.Question;
import com.adaptive.server.entity.StudentProgress;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.TutorChatMessage;
import com.adaptive.server.entity.User;
import com.adaptive.server.repository.QuestionRepository;
import com.adaptive.server.repository.StudentProgressRepository;
import com.adaptive.server.repository.TutorChatMessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TutorChatService {

    private static final int MAX_HISTORY_MESSAGES = 20;

    private final QuestionRepository questionRepository;
    private final StudentProgressRepository studentProgressRepository;
    private final TutorChatMessageRepository tutorChatMessageRepository;
    private final TutorChatClient tutorChatClient;

    public TutorChatService(QuestionRepository questionRepository,
                            StudentProgressRepository studentProgressRepository,
                            TutorChatMessageRepository tutorChatMessageRepository,
                            TutorChatClient tutorChatClient) {
        this.questionRepository = questionRepository;
        this.studentProgressRepository = studentProgressRepository;
        this.tutorChatMessageRepository = tutorChatMessageRepository;
        this.tutorChatClient = tutorChatClient;
    }

    @Transactional
    public TutorChatResponse askTutor(User user, TutorChatRequest request) {
        validateRequest(request);

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found."));

        SubSubject subSubject = question.getSubSubject();
        Optional<StudentProgress> progress =
                studentProgressRepository.findByUserIdAndSubSubjectId(user.getId(), subSubject.getId());

        List<TutorChatMessage> previousMessages =
                tutorChatMessageRepository.findByUserIdAndQuestionIdOrderByCreatedAtAsc(user.getId(), question.getId());

        TutorMicroserviceRequest microserviceRequest = new TutorMicroserviceRequest();
        microserviceRequest.setQuestion(toQuestionContext(question));
        microserviceRequest.setStudent(toStudentContext(user, progress));
        microserviceRequest.setStudentMessage(request.getMessage().trim());
        microserviceRequest.setConversationHistory(toConversationHistory(previousMessages));
        microserviceRequest.setGuidanceLevel(resolveGuidanceLevel(previousMessages));

        TutorMicroserviceResponse microserviceResponse = tutorChatClient.askTutor(microserviceRequest);
        TutorChatResponse response = new TutorChatResponse(
                microserviceResponse.getMessage(),
                microserviceResponse.getGuidanceLevel(),
                microserviceResponse.getAction()
        );

        tutorChatMessageRepository.save(new TutorChatMessage(
                user,
                question,
                subSubject,
                request.getMessage().trim(),
                response.getMessage(),
                response.getGuidanceLevel(),
                response.getAction(),
                LocalDateTime.now()
        ));

        return response;
    }

    @Transactional(readOnly = true)
    public List<TutorChatHistoryMessage> getHistory(User user, Long questionId) {
        List<TutorChatMessage> rows =
                tutorChatMessageRepository.findByUserIdAndQuestionIdOrderByCreatedAtAsc(user.getId(), questionId);

        List<TutorChatHistoryMessage> history = new ArrayList<>();
        for (TutorChatMessage message : rows) {
            history.add(new TutorChatHistoryMessage(
                    message.getStudentMessage(),
                    message.getTutorResponse(),
                    message.getGuidanceLevel(),
                    message.getAction(),
                    message.getCreatedAt()
            ));
        }
        return history;
    }

    @Transactional(readOnly = true)
    public List<TutorConversationSummary> getConversations(User user) {
        return tutorChatMessageRepository.findConversationSummaries(user.getId());
    }

    private void validateRequest(TutorChatRequest request) {
        if (request == null || request.getQuestionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "questionId is required.");
        }

        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message is required.");
        }
    }

    private TutorQuestionContext toQuestionContext(Question question) {
        TutorQuestionContext context = new TutorQuestionContext();
        SubSubject subSubject = question.getSubSubject();

        context.setQuestionId(question.getId());
        context.setExpression(question.getExpression());
        context.setCorrectAnswer(question.getCorrectAnswer());
        context.setSolutionSteps(question.getSolution());
        context.setOptions(question.getOptions());
        context.setSubSubject(subSubject.getName());
        context.setSubject(subSubject.getSubject().getName());
        context.setDifficultyLevel(question.getDifficultyLevel());
        context.setLanguage(normalizeLanguage(question.getLanguage()));
        context.setSource("MANUAL");

        return context;
    }

    private TutorStudentContext toStudentContext(User user, Optional<StudentProgress> progress) {
        TutorStudentContext context = new TutorStudentContext();
        context.setStudentId(user.getId());
        context.setAge(user.getAge());
        context.setCurrentLevel(progress.map(StudentProgress::getCurrentLevel).orElse(null));
        context.setWeaknessType(null);

        return context;
    }

    private List<ConversationMessage> toConversationHistory(List<TutorChatMessage> messages) {
        List<ConversationMessage> history = new ArrayList<>();

        for (TutorChatMessage message : messages) {
            history.add(new ConversationMessage("student", message.getStudentMessage()));
            history.add(new ConversationMessage("tutor", message.getTutorResponse()));
        }

        if (history.size() > MAX_HISTORY_MESSAGES) {
            return new ArrayList<>(history.subList(history.size() - MAX_HISTORY_MESSAGES, history.size()));
        }

        return history;
    }

    private int resolveGuidanceLevel(List<TutorChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }

        TutorChatMessage lastMessage = messages.get(messages.size() - 1);
        Integer lastGuidanceLevel = lastMessage.getGuidanceLevel();

        return lastGuidanceLevel == null ? 0 : lastGuidanceLevel;
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "he";
        }

        return language;
    }
}
