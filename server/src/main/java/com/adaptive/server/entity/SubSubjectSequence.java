package com.adaptive.server.entity;
import javax.persistence.*;

@Entity
@Table(name = "subsubject_sequence")
public class SubSubjectSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_id", nullable = false)
    private SubSubject currentSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_id", nullable = false)
    private SubSubject nextSubject;

    // Getters and Setters...
}
