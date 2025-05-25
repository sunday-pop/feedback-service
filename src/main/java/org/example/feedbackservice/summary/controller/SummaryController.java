package org.example.feedbackservice.summary.controller;

import lombok.RequiredArgsConstructor;
import org.example.feedbackservice.summary.model.dto.SummaryRequest;
import org.example.feedbackservice.summary.model.dto.SummaryResponse;
import org.example.feedbackservice.summary.model.dto.SummaryStatusResponse;
import org.example.feedbackservice.summary.service.SummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8090") // NOTE: 포트폴리오서비스 명시
public class SummaryController {

    private final SummaryService summaryService;

    // 요약 요청
    @PostMapping("/request")
    public ResponseEntity<Void> requestSummary(@RequestBody SummaryRequest request) {
        summaryService.handleSummary(request);
        return ResponseEntity.noContent().build();
    }

    // 요약 상태 확인
    @GetMapping("/{portfolioId}/status")
    public ResponseEntity<SummaryStatusResponse> checkSummaryStatus(@PathVariable String portfolioId) {
        SummaryStatusResponse status = summaryService.checkSummaryStatus(portfolioId);
        return ResponseEntity.ok().body(status);
    }

    // 요약 응답
    @GetMapping("/{portfolioId}")
    public ResponseEntity<SummaryResponse> getSummary(@PathVariable String portfolioId) {
        SummaryResponse summary = summaryService.getSummary(portfolioId);
        return ResponseEntity.ok().body(summary);
    }
}
