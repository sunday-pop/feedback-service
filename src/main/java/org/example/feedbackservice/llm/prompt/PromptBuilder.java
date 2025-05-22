package org.example.feedbackservice.llm.prompt;

import org.example.feedbackservice.llm.model.dto.GeminiRequest;

public interface PromptBuilder {
    GeminiRequest build(String content, String type);
}
