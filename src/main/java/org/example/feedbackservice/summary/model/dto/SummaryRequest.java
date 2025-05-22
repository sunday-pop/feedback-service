package org.example.feedbackservice.summary.model.dto;

import java.util.List;

public record SummaryRequest(
        String portfolioId,
        List<String> urls,
        List<String> fileUrls
) {
}
