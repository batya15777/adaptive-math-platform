package com.adaptive.server.service.admin;

import com.adaptive.server.DTOs.AdminSubSubjectDto;
import com.adaptive.server.DTOs.AdminSubjectSubSubjectsDto;
import com.adaptive.server.entity.SubSubject;
import com.adaptive.server.entity.Subject;
import com.adaptive.server.entity.Topic;
import com.adaptive.server.repository.QuestionTemplateRepository;
import com.adaptive.server.repository.SubSubjectRepository;
import com.adaptive.server.repository.SubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin content management for SubSubjects (under a Subject). Soft-delete / draft
 * semantics like Topic/Subject: never physically deleted — only activated/deactivated.
 *
 * Protection: sub-subjects whose parent subject is protected ("Calculation") are
 * locked (no create/rename/disable) because the question generators depend on their
 * names. External guard only — generators / LevelManagerService are not touched.
 */
@Service
public class AdminSubSubjectService {

    // Parent subject protected by name (case-insensitive).
    private static final String PROTECTED_SUBJECT = "Calculation";

    private final SubjectRepository subjectRepository;
    private final SubSubjectRepository subSubjectRepository;
    private final QuestionTemplateRepository questionTemplateRepository;

    public AdminSubSubjectService(SubjectRepository subjectRepository,
                                  SubSubjectRepository subSubjectRepository,
                                  QuestionTemplateRepository questionTemplateRepository) {
        this.subjectRepository = subjectRepository;
        this.subSubjectRepository = subSubjectRepository;
        this.questionTemplateRepository = questionTemplateRepository;
    }

    private boolean isProtectedParent(String subjectName) {
        return PROTECTED_SUBJECT.equalsIgnoreCase(subjectName);
    }

    @Transactional(readOnly = true)
    public AdminSubjectSubSubjectsDto getBySubject(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found."));
        Topic topic = subject.getTopic();
        boolean prot = isProtectedParent(subject.getName());

        List<AdminSubSubjectDto> items = subSubjectRepository.findBySubjectIdOrderByNameAsc(subjectId).stream()
                .map(ss -> new AdminSubSubjectDto(ss.getId(), subjectId, ss.getName(),
                        questionTemplateRepository.countBySubSubject_Id(ss.getId()),
                        ss.getActive(), prot))
                .collect(Collectors.toList());

        long activeCount = subSubjectRepository.countBySubjectIdAndActiveTrue(subjectId);
        return new AdminSubjectSubSubjectsDto(subjectId, subject.getName(), subject.getActive(),
                prot, topic.getId(), topic.getName(), activeCount, items);
    }

    @Transactional
    public AdminSubSubjectDto create(Long subjectId, String rawName) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found."));
        if (isProtectedParent(subject.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot add sub-subjects to a protected system subject.");
        }
        String name = normalize(rawName);
        if (subSubjectRepository.findByNameAndSubjectId(name, subjectId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sub-subject name already exists.");
        }
        SubSubject ss = new SubSubject(name, subject);
        ss.setActive(false); // new sub-subjects start as drafts, hidden from students
        SubSubject saved = subSubjectRepository.save(ss);
        return new AdminSubSubjectDto(saved.getId(), subjectId, saved.getName(), 0,
                saved.getActive(), false);
    }

    @Transactional
    public AdminSubSubjectDto rename(Long id, String rawName) {
        SubSubject ss = subSubjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sub-subject not found."));
        Subject subject = ss.getSubject();
        if (isProtectedParent(subject.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This sub-subject is protected and cannot be renamed.");
        }
        String name = normalize(rawName);
        if (!ss.getName().equalsIgnoreCase(name)
                && subSubjectRepository.findByNameAndSubjectId(name, subject.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sub-subject name already exists.");
        }
        ss.setName(name);
        SubSubject saved = subSubjectRepository.save(ss);
        return new AdminSubSubjectDto(saved.getId(), subject.getId(), saved.getName(),
                questionTemplateRepository.countBySubSubject_Id(id), saved.getActive(),
                isProtectedParent(subject.getName()));
    }

    @Transactional
    public AdminSubSubjectDto setActive(Long id, boolean active) {
        SubSubject ss = subSubjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sub-subject not found."));
        Subject subject = ss.getSubject();

        if (active) {
            // A sub-subject can only be published if it can actually produce questions,
            // i.e. it has at least one question template.
            if (questionTemplateRepository.countBySubSubject_Id(id) == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cannot publish a sub-subject with no question templates.");
            }
        } else {
            // Disabling.
            if (isProtectedParent(subject.getName())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This sub-subject is protected and cannot be disabled.");
            }
            // Keep the invariant "an active subject always has >= 1 active sub-subject":
            if (ss.getActive() && subject.getActive()
                    && subSubjectRepository.countBySubjectIdAndActiveTrue(subject.getId()) == 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cannot disable the last published sub-subject of an active subject. "
                                + "Disable the subject first or publish another sub-subject.");
            }
        }

        ss.setActive(active);
        SubSubject saved = subSubjectRepository.save(ss);
        return new AdminSubSubjectDto(saved.getId(), subject.getId(), saved.getName(),
                questionTemplateRepository.countBySubSubject_Id(id), saved.getActive(),
                isProtectedParent(subject.getName()));
    }

    private String normalize(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sub-subject name is required.");
        }
        return name.trim();
    }
}
