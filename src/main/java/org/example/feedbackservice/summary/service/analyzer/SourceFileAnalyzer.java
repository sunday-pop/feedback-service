package org.example.feedbackservice.summary.service.analyzer;

public interface SourceFileAnalyzer {
    boolean supports(String filename);
    String analyze(String content);
}
