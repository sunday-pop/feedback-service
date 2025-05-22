package org.example.feedbackservice.summary.model.repository;

import org.example.feedbackservice.summary.model.entity.PortfolioSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioSummaryRepository extends JpaRepository<PortfolioSummary, Long> {
    PortfolioSummary findByPortfolioId(String portfolioId);
}
