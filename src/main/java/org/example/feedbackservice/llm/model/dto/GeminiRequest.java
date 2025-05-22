package org.example.feedbackservice.llm.model.dto;

import java.util.List;

public record GeminiRequest(List<Content> contents) {
    public record Content(String role, List<Part> parts) {
    }

    public record Part(String text) {
    }
}
