package com.aug.resilience.cron.infra.logger;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class CircuitBreakerEventLogger {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

//    @PostConstruct
//    public void logCircuitBreakerTransitions() {
//        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
//            cb.getEventPublisher()
//                    .onStateTransition(event -> log.warn(
//                            "[CIRCUIT BREAKER][{}] TRANSITION {} → {}",
//                            event.getCircuitBreakerName(),
//                            event.getStateTransition().getFromState(),
//                            event.getStateTransition().getToState()
//                    ));
//        });
//    }

    @PostConstruct
    public void registerCircuitBreakerEvents() {
        log.info("CircuitBreakerEventLogger - Registrando listeners de eventos de circuit breaker");

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("processPayment");

        circuitBreaker.getEventPublisher()
                .onStateTransition(this::onStateTransition)
                .onCallNotPermitted(this::onCallNotPermitted)
                .onError(this::onError)
                .onSuccess(this::onSuccess);
    }

    private void onStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        log.warn("CB [{}] - STATE TRANSITION: {} -> {}",
                event.getCircuitBreakerName(),
                event.getStateTransition().getFromState(),
                event.getStateTransition().getToState());
    }

    private void onCallNotPermitted(CircuitBreakerOnCallNotPermittedEvent event) {
        log.error("CB [{}] - CALL BLOCKED (OPEN or DISABLED): Circuit breaker did not permit the call.",
                event.getCircuitBreakerName());
    }

    private void onError(CircuitBreakerOnErrorEvent event) {
        log.error("CB [{}] - ERROR: Duration={}ms, Exception={}",
                event.getCircuitBreakerName(),
                event.getElapsedDuration().toMillis(),
                event.getThrowable() != null ? event.getThrowable().getClass().getSimpleName() : "Unknown");
    }

    private void onSuccess(CircuitBreakerOnSuccessEvent event) {
        log.info("CB [{}] - SUCCESS: Duration={}ms",
                event.getCircuitBreakerName(),
                event.getElapsedDuration().toMillis());
    }
}
