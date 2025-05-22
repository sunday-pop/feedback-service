package org.example.feedbackservice.feedback.model.dto;

import org.example.feedbackservice.feedback.model.entity.FeedbackStatus;

public record FeedbackResponse(
        Long id,
        FeedbackStatus feedbackStatus,
        String llmFeedback
) {
}
