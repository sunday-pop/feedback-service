package org.example.feedbackservice.summary.service;

import org.example.feedbackservice.summary.model.dto.SummaryRequest;
import org.example.feedbackservice.summary.model.dto.SummaryResponse;
import org.example.feedbackservice.summary.model.dto.SummaryStatusResponse;

public interface SummaryService {
    void handleSummary(SummaryRequest request);
    SummaryResponse getSummary(String portfolioId);
    SummaryStatusResponse checkSummaryStatus(String portfolioId);
}
