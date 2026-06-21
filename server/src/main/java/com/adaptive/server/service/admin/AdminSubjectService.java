package com.adaptive.server.service.admin;

import com.adaptive.server.DTOs.AdminSubjectDto;
import com.adaptive.server.DTOs.AdminTopicSubjectsDto;
import com.adaptive.server.entity.Subject;
import com.adaptive.server.entity.Topic;
import com.adaptive.server.repository.SubSubjectRepository;
import com.adaptive.server.repository.SubjectRepository;
import com.adaptive.server.repository.TopicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin content management for Subjects (under a Topic). Soft-delete / draft
 * semantics like Topics: subjects are never physically deleted — only activated or
 * deactivated via the {@code active} flag.
 *
 * Protection: the "Calculation" subject is depended on by name elsewhere in the
 * system, so renaming/disabling it is blocked here (external guard only — the
 * question generators / LevelManagerService are not touched).
 */
@Service
public class AdminSubjectService {

    // Protected by name (case-insensitive). External admin guard only.
    private static final String PROTECTED_SUBJECT = "Calculation";

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final SubSubjectRepository subSubjectRepository;

    public AdminSubjectService(TopicRepository topicRepository,
                               SubjectRepository subjectRepository,
                               SubSubjectRepository subSubjectRepository) {
        this.topicRepository = topicRepository;
        this.subjectRepository = subjectRepository;
        this.subSubjectRepository = subSubjectRepository;
    }

    private boolean isProtected(String name) {
        return PROTECTED_SUBJECT.equalsIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public AdminTopicSubjectsDto getByTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found."));

        List<AdminSubjectDto> subjects = subjectRepository.findByTopicIdOrderByNameAsc(topicId).stream()
                .map(s -> new AdminSubjectDto(s.getId(), topicId, s.getName(),
                        subSubjectRepository.countBySubjectId(s.getId()),
                        subSubjectRepository.countBySubjectIdAndActiveTrue(s.getId()),
                        s.getActive(), isProtected(s.getName())))
                .collect(Collectors.toList());

        long activeSubjectCount = subjectRepository.countByTopicIdAndActiveTrue(topicId);
        return new AdminTopicSubjectsDto(topicId, topic.getName(), topic.getActive(),
                activeSubjectCount, subjects);
    }

    @Transactional
    public AdminSubjectDto create(Long topicId, String rawName) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found."));
        String name = normalize(rawName);
        if (subjectRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject name already exists.");
        }
        Subject subject = new Subject(name);
        subject.setTopic(topic);
        subject.setActive(false); // new subjects start as drafts, hidden from students
        Subject saved = subjectRepository.save(subject);
        return new AdminSubjectDto(saved.getId(), topicId, saved.getName(), 0, 0,
                saved.getActive(), isProtected(saved.getName()));
    }

    @Transactional
    public AdminSubjectDto rename(Long id, String rawName) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found."));
        if (isProtected(subject.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This subject is protected and cannot be renamed.");
        }
        String name = normalize(rawName);
        if (!subject.getName().equalsIgnoreCase(name) && subjectRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject name already exists.");
        }
        subject.setName(name);
        Subject saved = subjectRepository.save(subject);
        return new AdminSubjectDto(saved.getId(), saved.getTopic().getId(), saved.getName(),
                subSubjectRepository.countBySubjectId(id),
                subSubjectRepository.countBySubjectIdAndActiveTrue(id),
                saved.getActive(), isProtected(saved.getName()));
    }

    @Transactional
    public AdminSubjectDto setActive(Long id, boolean active) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found."));
        Long topicId = subject.getTopic().getId();

        if (active) {
            // A subject needs at least one PUBLISHED sub-subject before students see it.
            if (subSubjectRepository.countBySubjectIdAndActiveTrue(id) == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cannot publish a subject without a published sub-subject.");
            }
        } else {
            // Disabling.
            if (isProtected(subject.getName())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This subject is protected and cannot be disabled.");
            }
            // Keep the invariant "an active topic always has >= 1 active subject":
            // block disabling the last published subject of an active topic.
            if (subject.getActive() && subject.getTopic().getActive()
                    && subjectRepository.countByTopicIdAndActiveTrue(topicId) == 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cannot disable the last published subject of an active topic. "
                                + "Disable the topic first or publish another subject.");
            }
        }

        subject.setActive(active);
        Subject saved = subjectRepository.save(subject);
        return new AdminSubjectDto(saved.getId(), topicId, saved.getName(),
                subSubjectRepository.countBySubjectId(id),
                subSubjectRepository.countBySubjectIdAndActiveTrue(id),
                saved.getActive(), isProtected(saved.getName()));
    }

    private String normalize(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject name is required.");
        }
        return name.trim();
    }
}
