package org.example.feedbackservice.feedback.model.repository;

import org.example.feedbackservice.feedback.model.dto.FeedbackResponse;
import org.example.feedbackservice.feedback.model.entity.PortfolioFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PortfolioFeedbackRepository extends JpaRepository<PortfolioFeedback, Long> {

    @Query("SELECT new org.example.feedbackservice.feedback.model.dto.FeedbackResponse(p.id, p.status, p.llmFeedback) " +
            "FROM PortfolioFeedback p WHERE p.portfolioId = :portfolioId")
    List<FeedbackResponse> findFeedbackAndStatusByPortfolioId(@Param("portfolioId") String portfolioId);

    @Query("SELECT new org.example.feedbackservice.feedback.model.dto.FeedbackResponse(p.id, p.status, p.llmFeedback) " +
            "FROM PortfolioFeedback p WHERE p.id = :id")
    FeedbackResponse findFeedbackAndStatusById(@Param("id") Long id);

    @Query("SELECT new org.example.feedbackservice.feedback.model.dto.FeedbackResponse(p.id, p.status, p.llmFeedback) " +
            "FROM PortfolioFeedback p WHERE p.portfolioId = :portfolioId AND p.noteId = :noteId")
    FeedbackResponse findLatestFeedback(@Param("portfolioId") String portfolioId, @Param("noteId") Long noteId);

    PortfolioFeedback findTopByPortfolioIdOrderByCreatedAtDesc(String portfolioId);

    @Query(value = "SELECT pf.id, pf.created_at, pf.llm_feedback, pf.note_id, pf.portfolio_id, pf.status FROM portfolio_feedback pf WHERE pf.portfolio_id = :portfolioId ORDER BY pf.created_at DESC LIMIT 1 OFFSET 1", nativeQuery = true)
    PortfolioFeedback findSecondLatestByPortfolioId(@Param("portfolioId") String portfolioId);

}
