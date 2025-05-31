package com.aug.resilience.cron.infra.logger;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnRetryEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class RetryEventLogger {

    private final RetryRegistry retryRegistry;

//    @PostConstruct
//    public void logRetryAttempts() {
//        retryRegistry.getAllRetries().forEach(retry -> {
//            retry.getEventPublisher()
//                    .onRetry(event -> log.info(
//                            "[RETRY][{}] Attempt #{} - Cause: {}",
//                            event.getName(),
//                            event.getNumberOfRetryAttempts(),
//                            event.getLastThrowable() != null
//                                    ? event.getLastThrowable().getClass().getSimpleName()
//                                    : "unknown"
//                    ));
//        });
//    }

    @PostConstruct
    public void registerRetryEvents() {
        log.info("RetryEventLogger - Registrando listeners de eventos de retry");

        Retry retry = retryRegistry.find("processPaymentRetry")
                .orElseThrow(() -> new IllegalArgumentException("Retry instance not found"));

        retry.getEventPublisher().onRetry(event -> {
            RetryOnRetryEvent e = (RetryOnRetryEvent) event;
            log.warn("RETRY [{}] - Attempt #{}, Reason: {}",
                    e.getName(),
                    e.getNumberOfRetryAttempts(),
                    e.getLastThrowable() != null ? e.getLastThrowable().getMessage() : "Unknown");
        });

        retry.getEventPublisher().onError(event -> {
            RetryOnErrorEvent e = (RetryOnErrorEvent) event;
            log.error("RETRY [{}] - Final failure after {} attempts. Reason: {}",
                    e.getName(),
                    e.getNumberOfRetryAttempts(),
                    e.getLastThrowable() != null ? e.getLastThrowable().getMessage() : "Unknown");
        });

        retry.getEventPublisher().onSuccess(event -> {
            RetryOnSuccessEvent e = (RetryOnSuccessEvent) event;
            log.info("RETRY [{}] - Success after {} attempt(s)",
                    e.getName(),
                    e.getNumberOfRetryAttempts());
        });
    }
}