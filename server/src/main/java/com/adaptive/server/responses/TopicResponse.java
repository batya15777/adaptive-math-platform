package com.adaptive.server.responses;

import com.adaptive.server.entity.Topic;

public class TopicResponse {

    private Long id;
    private String name;

    public TopicResponse() {}

    public TopicResponse(Topic topic) {
        this.id   = topic.getId();
        this.name = topic.getName();
    }

    public Long getId()      { return id; }
    public String getName()  { return name; }

    public void setId(Long id)        { this.id = id; }
    public void setName(String name)  { this.name = name; }
}
