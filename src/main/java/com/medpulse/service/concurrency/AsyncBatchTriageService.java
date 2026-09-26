package com.medpulse.service.concurrency;

import com.medpulse.model.PatientTriageCase;
import com.medpulse.model.TriageStatus;
import com.medpulse.repository.PatientTriageCaseRepository;
import com.medpulse.service.strategy.ScoringStrategyContext;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Concurrency & Multithreading Service:
 * Executes asynchronous parallel triage risk assessment for batch patient arrivals
 * using CompletableFuture and a dedicated custom ThreadPoolExecutor.
 */
@Service
@Slf4j
public class AsyncBatchTriageService {

    private static final Pattern SPO2_PATTERN = Pattern.compile("(?i)SpO2\\s*:\\s*(\\d+)");

    private final PatientTriageCaseRepository patientCaseRepository;
    private final ScoringStrategyContext scoringContext;
    private final ExecutorService triageExecutor;

    public AsyncBatchTriageService(
            PatientTriageCaseRepository patientCaseRepository,
            ScoringStrategyContext scoringContext) {
        this.patientCaseRepository = patientCaseRepository;
        this.scoringContext = scoringContext;

        AtomicInteger threadCount = new AtomicInteger(1);
        this.triageExecutor = new ThreadPoolExecutor(
                4,
                8,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("medpulse-triage-worker-" + threadCount.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * Batch screens and risk-scores incoming patient cases in parallel
     */
    public CompletableFuture<List<PatientTriageCase>> processBatchTriageAsync(List<PatientTriageCase> cases) {
        log.info("Initiating concurrent batch triage for {} patients on thread pool", cases.size());

        List<CompletableFuture<PatientTriageCase>> futures = cases.stream()
                .map(patientCase -> CompletableFuture.supplyAsync(() -> {
                    try {
                        int score = scoringContext.calculateBlendedRiskScore(patientCase, patientCase.getDepartment());
                        String acuity = ScoringStrategyContext.determineAcuityLevel(score);

                        patientCase.setTriageRiskScore(score);
                        patientCase.setAiAcuityLevel(acuity);
                        patientCase.setTriageStatus(TriageStatus.AI_EVALUATED);

                        // Fast rule-based risk flags
                        StringBuilder flags = new StringBuilder();
                        String vitals = patientCase.getVitalSigns() != null ? patientCase.getVitalSigns() : "";
                        Matcher spo2Matcher = SPO2_PATTERN.matcher(vitals);
                        if (spo2Matcher.find()) {
                            try {
                                int spo2 = Integer.parseInt(spo2Matcher.group(1));
                                if (spo2 <= 93) {
                                    flags.append("Hypoxemia Alert (SpO2: ").append(spo2).append("%); ");
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                        if (score >= 80) {
                            flags.append("High Critical Risk Protocol Triggered; ");
                        }
                        if (patientCase.getAllergies() != null && !patientCase.getAllergies().isBlank() && !patientCase.getAllergies().equalsIgnoreCase("None")) {
                            flags.append("Allergy Caution: ").append(patientCase.getAllergies()).append("; ");
                        }

                        if (!flags.isEmpty()) {
                            patientCase.setAiRiskFlags(flags.toString());
                        }

                        return patientCaseRepository.save(patientCase);
                    } catch (Exception e) {
                        log.error("Error in parallel triage evaluation for patient {}: {}", patientCase.getPatientId(), e.getMessage());
                        return patientCase;
                    }
                }, triageExecutor))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    @PreDestroy
    public void cleanup() {
        triageExecutor.shutdown();
        try {
            if (!triageExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                triageExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            triageExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
