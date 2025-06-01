package org.example.feedbackservice.feedback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.example.feedbackservice.feedback.model.dto.BatchFeedbackRequest;
import org.example.feedbackservice.feedback.model.dto.FeedbackRequest;
import org.example.feedbackservice.feedback.model.dto.FeedbackResponse;
import org.example.feedbackservice.feedback.model.entity.FeedbackStatus;
import org.example.feedbackservice.feedback.model.entity.PortfolioFeedback;
import org.example.feedbackservice.feedback.model.repository.PortfolioFeedbackRepository;
import org.example.feedbackservice.common.exception.FeedbackNotFoundException;
import org.example.feedbackservice.common.exception.NotCompletedSummaryException;
import org.example.feedbackservice.common.exception.SummaryNotFoundException;
import org.example.feedbackservice.llm.client.LLMClient;
import org.example.feedbackservice.summary.model.entity.PortfolioSummary;
import org.example.feedbackservice.summary.model.entity.SummaryStatus;
import org.example.feedbackservice.summary.model.repository.PortfolioSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log
public class FeedbackServiceImpl implements FeedbackService {

    private final PortfolioSummaryRepository summaryRepository;
    private final PortfolioFeedbackRepository portfolioFeedbackRepository;
    private final LLMClient llmClient;

    @Override
    @Transactional(rollbackFor = Exception.class)  // 모든 예외 발생 시 롤백
    public FeedbackResponse generateFeedback(FeedbackRequest request) {

        PortfolioSummary summary = summaryRepository.findByPortfolioId(request.portfolioId());
        if (summary == null) {
            throw new SummaryNotFoundException("요약이 존재하지 않습니다.");
        }

        if (!summary.getStatus().equals(SummaryStatus.COMPLETED)) {
            log.info("요약이 완료되지 않았습니다.");
            throw new NotCompletedSummaryException("요약이 완료되지 않았습니다.");
        }

        String finalSummary = summary.getFinalSummary();
        PortfolioFeedback prevFeedback = portfolioFeedbackRepository.findTopByPortfolioIdOrderByCreatedAtDesc(request.portfolioId());

        // 추가 데이터 (노트내용)
        String noteContent = request.noteContent();
        String data = """
                    [노트 내용] %s
                    [포트폴리오 요약] %s
                    """.formatted(noteContent, finalSummary);
        boolean isFirst = true;

        // 이전 피드백 유무에 따라 분기
        if (prevFeedback != null) {
            String prev = prevFeedback.getLlmFeedback();
            log.info("prev = " + prev);
            data += """
                    [이전 피드백] %s
                    """.formatted(prev);
            isFirst = false;
        }

        log.info("data = " + data);

        PortfolioFeedback feedback = generateFeedbackLLM(request.portfolioId(), request.noteId(), data, isFirst);
        return getFeedback(feedback.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbackList(String portfolioId) {
        List<FeedbackResponse> feedbackResponses = portfolioFeedbackRepository.findFeedbackAndStatusByPortfolioId(portfolioId);
        if (feedbackResponses.stream().anyMatch(feedbackResponse -> feedbackResponse.feedbackStatus() == null)) {
            throw new FeedbackNotFoundException("등록된 피드백이 없습니다.");
        }
        return feedbackResponses;
    }

    @Override
    @Transactional(readOnly = true)  // 읽기 전용 트랜잭션
    public FeedbackResponse getFeedback(Long feedbackId) {
        return portfolioFeedbackRepository.findFeedbackAndStatusById(feedbackId);
    }

    @Override
    @Transactional(readOnly = true)  // 읽기 전용 트랜잭션
    public FeedbackResponse getLatestFeedback(String portfolioId, Long noteId) {
        FeedbackResponse latest = portfolioFeedbackRepository.findLatestFeedback(portfolioId, noteId);
        if (latest == null) {
            throw new FeedbackNotFoundException("등록된 피드백이 없습니다.");
        }
        return latest;
    }

    @Transactional
    protected PortfolioFeedback generateFeedbackLLM(String portfolioId, Long noteId, String data, boolean isFirst) {
        PortfolioFeedback feedback = new PortfolioFeedback();
        feedback.setPortfolioId(portfolioId);
        feedback.setNoteId(noteId);
        feedback.setStatus(FeedbackStatus.IN_PROCESSING);
        portfolioFeedbackRepository.save(feedback); // 상태 업데이트

        String type = isFirst ? "first_develop" : "develop";
        llmClient.feedback(data, type)
                .doOnNext(result -> {
                    log.info("feedback = " + result);
                    feedback.setLlmFeedback(result);
                    feedback.setStatus(FeedbackStatus.COMPLETED);
                    portfolioFeedbackRepository.save(feedback); // 상태 업데이트
                })
                .doOnError(error -> {
                    log.warning("피드백 생성 중 오류 발생 : " + error.getMessage());
                    feedback.setStatus(FeedbackStatus.FAILED);
                    portfolioFeedbackRepository.save(feedback); // 상태 업데이트
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
        return feedback;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getBatchLatestFeedbacks(BatchFeedbackRequest request) {
        List<FeedbackResponse> results = new ArrayList<>();

        for (Long noteId : request.noteIds()) {
            try {
                FeedbackResponse feedback = getLatestFeedback(request.portfolioId(), noteId);
                if (feedback != null) {
                    results.add(feedback);
                }
            } catch (Exception e) {
                log.warning("배치 처리 중 개별 피드백 조회 실패 - noteId: %s".formatted(noteId));
                log.warning(e.getMessage());
            }
        }
        return results;
    }
}