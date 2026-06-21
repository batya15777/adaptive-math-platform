package com.adaptive.server.service.admin;

import com.adaptive.server.DTOs.AdminTopicDto;
import com.adaptive.server.entity.Topic;
import com.adaptive.server.repository.SubjectRepository;
import com.adaptive.server.repository.TopicRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin content management for Topics. Uses soft-delete / draft semantics:
 * topics are never physically deleted — they are activated (shown to students)
 * or deactivated (hidden) via the {@code active} flag.
 */
@Service
public class AdminTopicService {

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;

    public AdminTopicService(TopicRepository topicRepository, SubjectRepository subjectRepository) {
        this.topicRepository = topicRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminTopicDto> getTopics() {
        return topicRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(t -> new AdminTopicDto(t.getId(), t.getName(),
                        subjectRepository.countByTopicId(t.getId()), t.getActive()))
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminTopicDto create(String rawName) {
        String name = normalize(rawName);
        if (topicRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Topic name already exists.");
        }
        Topic topic = new Topic(name);
        topic.setActive(false); // new topics start as drafts, hidden from students
        Topic saved = topicRepository.save(topic);
        return new AdminTopicDto(saved.getId(), saved.getName(), 0, saved.getActive());
    }

    @Transactional
    public AdminTopicDto rename(Long id, String rawName) {
        String name = normalize(rawName);
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found."));
        if (!topic.getName().equalsIgnoreCase(name) && topicRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Topic name already exists.");
        }
        topic.setName(name);
        Topic saved = topicRepository.save(topic);
        return new AdminTopicDto(saved.getId(), saved.getName(),
                subjectRepository.countByTopicId(id), saved.getActive());
    }

    @Transactional
    public AdminTopicDto setActive(Long id, boolean active) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found."));
        long subjectCount = subjectRepository.countByTopicId(id);
        // Disabling is always allowed; publishing an empty topic is not — students
        // must never see a topic with no subjects.
        if (active && subjectCount == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot publish a topic with no subjects. Add subjects first.");
        }
        topic.setActive(active);
        Topic saved = topicRepository.save(topic);
        return new AdminTopicDto(saved.getId(), saved.getName(), subjectCount, saved.getActive());
    }

    private String normalize(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic name is required.");
        }
        return name.trim();
    }
}
