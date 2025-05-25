package org.example.feedbackservice.feedback.controller;

import lombok.RequiredArgsConstructor;
import org.example.feedbackservice.feedback.model.dto.FeedbackRequest;
import org.example.feedbackservice.feedback.model.dto.FeedbackResponse;
import org.example.feedbackservice.feedback.service.FeedbackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8090") // NOTE:포트폴리오서비스 주소 명시
public class FeedbackController {

    private final FeedbackService feedbackService;

    // 피드백 생성 요청
    @PostMapping("/request")
    public ResponseEntity<FeedbackResponse> requestFeedback(@RequestBody FeedbackRequest request) {
        FeedbackResponse response = feedbackService.generateFeedback(request);
        return ResponseEntity.ok(response);
    }

    // 피드백 조회
    @GetMapping("/{feedbackId}")
    public ResponseEntity<FeedbackResponse> getFeedback(@PathVariable Long feedbackId) {
        FeedbackResponse response = feedbackService.getFeedback(feedbackId);
        return ResponseEntity.ok(response);
    }

    // 최신 피드백 조회
    @GetMapping("/{portfolioId}/{noteId}/latest")
    public ResponseEntity<FeedbackResponse> getLatestFeedback(
            @PathVariable String portfolioId,
            @PathVariable Long noteId) {

        FeedbackResponse response = feedbackService.getLatestFeedback(portfolioId, noteId);
        return ResponseEntity.ok(response);
    }


    // 피드백 목록 조회
    @GetMapping("/portfolios/{portfolioId}")
    public ResponseEntity<List<FeedbackResponse>> feedbackList(@PathVariable String portfolioId) {
        List<FeedbackResponse> feedbackList = feedbackService.getFeedbackList(portfolioId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(feedbackList);
    }


}
