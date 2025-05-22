package org.example.feedbackservice.summary.model.dto;

import lombok.Data;

@Data
public class GitHubSummaryDTO {
    private String directoryTree;
    private String languages;
    private String codeStructure;
    private String readmeSummary;
    private String commitSummary;
    private String ciCd;
    private String finalSummary;
}
