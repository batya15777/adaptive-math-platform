package com.adaptive.server.responses;

import com.adaptive.server.entity.Subject;

public class SubjectResponse {

    private Long id;
    private String name;

    public SubjectResponse() {}

    public SubjectResponse(Subject subject) {
        this.id   = subject.getId();
        this.name = subject.getName();
    }

    public Long getId()      { return id; }
    public String getName()  { return name; }

    public void setId(Long id)        { this.id = id; }
    public void setName(String name)  { this.name = name; }
}
