package org.example.feedbackservice.feedback.service;

import org.example.feedbackservice.feedback.model.dto.FeedbackRequest;
import org.example.feedbackservice.feedback.model.dto.FeedbackResponse;

import java.util.List;

public interface FeedbackService {

    FeedbackResponse generateFeedback(FeedbackRequest request);

    List<FeedbackResponse> getFeedbackList(String portfolioId);

    FeedbackResponse getFeedback(Long feedbackId);

    FeedbackResponse getLatestFeedback(String portfolioId, Long noteId);

}
