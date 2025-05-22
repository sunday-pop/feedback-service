package org.example.feedbackservice.llm.client;

import lombok.RequiredArgsConstructor;
import org.example.feedbackservice.llm.prompt.FeedbackPromptBuilder;
import org.example.feedbackservice.llm.model.dto.GeminiRequest;
import org.example.feedbackservice.llm.model.dto.GeminiResponse;
import org.example.feedbackservice.llm.prompt.SummaryPromptBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LLMClient {

    private final FeedbackPromptBuilder feedbackPromptBuilder;
    private final SummaryPromptBuilder summaryPromptBuilder;

    @Value("${llm.api.key}")
    private String apiKey;

    @Value("${llm.api.url}")
    private String apiUrl;

    private WebClient createWebClient() {
        return WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // 토큰 수 확인
    public Mono<Integer> countTokens(String text) {
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        content.put("parts", new Object[]{Map.of("text", text)});
        requestBody.put("contents", new Object[]{content});

        return createWebClient()
                .post()
                .uri(":countTokens?key=%s", apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Integer) ((Map<?, ?>) response.get("usage")).get("totalTokens"))
                .onErrorResume(e -> {
                    System.err.println("토큰 수 계산 중 오류 발생: " + e.getMessage());
                    return Mono.error(e);
                });
    }

    // LLM 피드백 요청
    public Mono<String> feedback(String content, String type) {
        GeminiRequest feedbackRequest = feedbackPromptBuilder.build(content, type);
        return createWebClient()
                .post()
//                .uri("?key=%s".formatted(apiKey))
                .uri(":generateContent?key=%s".formatted(apiKey))
                .bodyValue(feedbackRequest)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .map(response -> response.candidates().get(0).content().parts().get(0).text());
    }

    // LLM 요약 요청
    public Mono<String> summarize(String content, String type) {
        GeminiRequest summaryRequest = summaryPromptBuilder.build(content, type);
        return createWebClient()
                .post()
//                .uri("?key=%s".formatted(apiKey))
                .uri(":generateContent?key=%s".formatted(apiKey))
                .bodyValue(summaryRequest)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .map(response -> response.candidates().get(0).content().parts().get(0).text());
    }


}