package org.example.feedbackservice.summary.model.dto;

import java.util.List;

public record SummaryCheckRequest(
        String portfolioId,
        List<String> urls
) {
}
