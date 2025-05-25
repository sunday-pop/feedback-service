package org.example.feedbackservice.feedback.model.dto;

public record FeedbackRequest(
        String portfolioId,
        Long noteId,
        String noteContent
) {
}
