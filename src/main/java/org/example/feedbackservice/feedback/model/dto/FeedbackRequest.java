package org.example.feedbackservice.feedback.model.dto;

public record FeedbackRequest(
        String portfolioId,
        String description, // 추후에 요약에서...
        Long noteId,
        String noteContent
) {
}
