package org.example.feedbackservice.summary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.example.feedbackservice.global.exception.SummaryNotFoundException;
import org.example.feedbackservice.llm.service.LLMSummaryService;
import org.example.feedbackservice.summary.model.dto.SummaryRequest;
import org.example.feedbackservice.summary.model.dto.SummaryResponse;
import org.example.feedbackservice.summary.model.dto.SummaryStatusResponse;
import org.example.feedbackservice.summary.model.entity.PortfolioSummary;
import org.example.feedbackservice.summary.model.entity.SummaryStatus;
import org.example.feedbackservice.summary.model.repository.PortfolioSummaryRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log
public class SummaryServiceImpl implements SummaryService {

    public static final int MAX_SAFE_LENGTH = 5000;

    private final PortfolioSummaryRepository summaryRepository;
    private final GitHubService gitHubService;
    private final FileReadService fileReadService;
    private final LLMSummaryService llmSummaryService;

    /**
     * 포트폴리오 분석 처리 (쓰기 작업)
     * - 트랜잭션 범위: DB 상태 변경까지 포함
     * - rollbackFor: 모든 예외 발생 시 롤백
     * - Reactor 스케줄러와의 충돌 방지를 위해 트랜잭션 분리
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleSummary(SummaryRequest request) {
        PortfolioSummary portfolioSummary = initSummaryProcess(request.portfolioId());
        AnalysisData data = extractAnalysisData(request);

        if (data.isEmpty()) {
            abortSummary(portfolioSummary);
            return;
        }
        // 요약 시작
        startAsyncAnalysis(portfolioSummary, data);
    }

    @Override
    public SummaryResponse getSummary(String portfolioId) {
        PortfolioSummary summary = summaryRepository.findByPortfolioId(portfolioId);
        return new SummaryResponse(summary.getFinalSummary());
    }

    @Override
    public SummaryStatusResponse checkSummaryStatus(String portfolioId) {
        PortfolioSummary summary = summaryRepository.findByPortfolioId(portfolioId);
        if (summary == null) {
            throw new SummaryNotFoundException("요약 정보를 찾을 수 없습니다.");
        }
        return new SummaryStatusResponse(summary.getStatus());
    }

    public void checkGitHubUpdate(SummaryRequest request) {
        // 깃허브 링크 유무 확인
        PortfolioSummary summary = summaryRepository.findByPortfolioId(request.portfolioId());

        request.urls().stream()
                .filter(url -> url != null && url.contains("github.com"))
                .findFirst()
                .map(url -> gitHubService.isAfterLastCommit(url, summary.getCreatedAt().toInstant()))
                .ifPresent(isAfterLastCommit -> {
                    if (isAfterLastCommit) {
                        // 깃허브 분석 및 요약 프로세스
                    }
                });
    }

    /**
     * 비동기 분석 시작
     * - 트랜잭션 외부에서 실행 (Reactor 스케줄러와의 충돌 방지)
     * - 각 단계별 상태 업데이트는 별도 트랜잭션으로 처리
     */
    private void startAsyncAnalysis(PortfolioSummary summary, AnalysisData data) {
        log.info("startAsyncAnalysis");
        extractGithubTexts(data.githubUrls)
                .flatMap(githubTexts ->
                        Mono.zip(
                                processGithubData(summary, githubTexts),
                                processFileData(summary, data.fileUrls)
                        )
                )
                .flatMap(tuple -> generateFinalSummary(data.description, tuple.getT1(), tuple.getT2()))
                .doOnSuccess(finalSummary -> completeSummary(summary, finalSummary))
                .doOnError(error -> failSummary(summary, error))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    /**
     * 최종 요약 생성
     */
    private Mono<String> generateFinalSummary(String description, String githubSummary, String fileSummary) {
        String combined = description + "\n" + githubSummary + "\n" + fileSummary;
        return (combined.length() > MAX_SAFE_LENGTH)
                ? llmSummaryService.summarizeCombinedText(combined)
                : Mono.just(combined);
    }

    /**
     * 분석 완료 처리
     */
    private void completeSummary(PortfolioSummary summary, String finalSummary) {
        updateSummaryStatus(
                summary.getId(),
                SummaryStatus.COMPLETED,
                s -> s.setFinalSummary(finalSummary)
        );
        log.info("포트폴리오 분석 완료: " + summary.getId());
    }

    /**
     * 분석 실패 처리
     */
    private void failSummary(PortfolioSummary summary, Throwable error) {
        log.warning("분석 실패: " + error.getMessage());
        updateSummaryStatus(
                summary.getId(),
                SummaryStatus.FAILED,
                s -> {
                }
        );
    }

    /**
     * 상태 업데이트 공통 처리 (별도 트랜잭션)
     */
    @Transactional
    protected void updateSummaryStatus(Long summaryId, SummaryStatus status, java.util.function.Consumer<PortfolioSummary> updater) {
        int maxAttempts = 3;
        int attempt = 0;
        boolean success = false;

        while (attempt < maxAttempts && !success) {
            try {
                summaryRepository.findById(summaryId).ifPresent(summary -> {
                    summary.setStatus(status);
                    updater.accept(summary);
                    summaryRepository.save(summary);
                });
                success = true;
            } catch (ObjectOptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= maxAttempts) {
                    throw e; // 최대 시도 횟수 초과 시 예외 전파
                }
                try {
                    Thread.sleep(100L * attempt); // 점진적 대기
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 중단", ie);
                }
            }
        }
    }

    /**
     * 파일 데이터 요약
     */
    private Mono<String> processFileData(PortfolioSummary portfolioSummary, List<String> texts) {
        return llmSummaryService.summarizeFileText(texts)
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(summary -> {
                    log.info("fileSummary = " + summary);
                    portfolioSummary.setFileSummary(summary);
                    portfolioSummary.setStatus(SummaryStatus.COMBINED_IN_PROCESSING);
                    summaryRepository.save(portfolioSummary);
                });
    }

    /**
     * GitHub 데이터 요약
     */
    private Mono<String> processGithubData(PortfolioSummary portfolioSummary, List<String> texts) {
        return llmSummaryService.summarizeGithubText(texts)
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(summary -> {
                    log.info("gitHubSummary = " + summary);
                    portfolioSummary.setGithubSummary(summary);
                    portfolioSummary.setStatus(SummaryStatus.FILE_IN_PROCESSING);
                    summaryRepository.save(portfolioSummary);
                });
    }

    /**
     * 분석 중단 처리
     */
    private void abortSummary(PortfolioSummary summary) {
        log.info("abortSummary!");
        summary.setStatus(SummaryStatus.NOT_STARTED);
        summaryRepository.save(summary);
    }

    /**
     * 분석 데이터 추출 (읽기 전용)
     */
    private AnalysisData extractAnalysisData(SummaryRequest request) {
        return new AnalysisData(
                request.description(),
                request.urls(),
                extractFileTexts(request.fileUrls())
        );
    }

    /**
     * 요약 프로세스 초기화
     */
    private PortfolioSummary initSummaryProcess(String portfolioId) {
        PortfolioSummary summary = new PortfolioSummary();
        summary.setPortfolioId(portfolioId);
        summary.setStatus(SummaryStatus.GITHUB_IN_PROCESSING);

        if (summary.getCreatedAt() == null) {
            summary.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
        }
        return summaryRepository.save(summary);
    }

    /**
     * 파일 텍스트 추출 (읽기 전용)
     */
    @Transactional(readOnly = true)
    protected List<String> extractFileTexts(List<String> fileUrls) {
        log.info("extractFileTexts : " + fileUrls.toString());
        // 파일이 있는지 확인
        if (fileUrls == null || fileUrls.isEmpty()) {
            return Collections.emptyList();
        }
        return fileReadService.extractTextsFromFiles(fileUrls);
    }

    /**
     * GitHub 텍스트 추출 (읽기 전용)
     */
    @Transactional(readOnly = true)
    protected Mono<List<String>> extractGithubTexts(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }
        return urls.stream()
                .filter(url -> url != null && url.contains("github.com"))
                .findFirst()
                .map(url -> gitHubService.summarizeProject(url)
                        .map(dto -> {
                            List<String> result = new ArrayList<>();
                            result.add("[디렉터리 구조] %s".formatted(dto.getDirectoryTree()));
                            result.add("[언어 구조] %s".formatted(dto.getLanguages()));
                            result.add("[코드 구조] %s".formatted(dto.getCodeStructure()));
                            result.add("[README 요약] %s".formatted(dto.getReadmeSummary()));
                            result.add("[최근 커밋] %s".formatted(dto.getCommitSummary()));
                            result.add("[CI/CD] %s".formatted(dto.getCiCd()));
                            log.info("result = " + result);
                            return result;
                        }))
                .orElse(Mono.just(Collections.emptyList()));
    }

    private record AnalysisData(String description, List<String> githubUrls, List<String> fileUrls) {
        boolean isEmpty() {
            return description == null && (githubUrls == null || githubUrls.isEmpty())
                    && (fileUrls == null || fileUrls.isEmpty());
        }
    }
}
