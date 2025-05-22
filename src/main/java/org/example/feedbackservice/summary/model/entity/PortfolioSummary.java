package org.example.feedbackservice.summary.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String portfolioId;

    @Lob
    private String githubSummary;

    @Lob
    private String fileSummary;

    @Lob
    private String finalSummary;

    @Enumerated(EnumType.STRING)
    private SummaryStatus status;

    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneOffset.UTC);
}