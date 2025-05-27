package org.example.feedbackservice.feedback.model.dto;

import java.util.List;

public record BatchFeedbackRequest(
        String portfolioId,
        List<Long> noteIds
) {
}
