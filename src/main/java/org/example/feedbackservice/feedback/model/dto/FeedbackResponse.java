package org.example.feedbackservice.feedback.model.dto;

import org.example.feedbackservice.feedback.model.entity.FeedbackStatus;

public record FeedbackResponse(
        Long id,
        Long noteId,
        FeedbackStatus feedbackStatus,
        String llmFeedback
) {
}
