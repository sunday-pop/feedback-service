package org.example.feedbackservice.feedback.model.entity;

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
public class PortfolioFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String portfolioId; // UUID
    private Long noteId;

    @Lob
    private String llmFeedback;

    @Enumerated(EnumType.STRING)
    private FeedbackStatus status;

    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneOffset.UTC);
}