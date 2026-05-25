package com.adaptive.server.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

// Tells Jackson to ignore null fields so JSON stays clean
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BasicResponse<T> {

    private boolean success;
    private String errorMessage;
    private T data;
    private LocalDateTime timestamp;

    // Default constructor
    public BasicResponse() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor for SUCCESS responses with data
    public BasicResponse(T data) {
        this.success = true;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    // Constructor for ERROR responses
    public BasicResponse(boolean success, Error error) {
        this.success = success;
        this.errorMessage = error.getMessage();
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}