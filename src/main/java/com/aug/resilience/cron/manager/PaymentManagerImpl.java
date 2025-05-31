package com.aug.resilience.cron.manager;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Random;
import java.util.concurrent.TimeoutException;

@Component
@Log4j2
public class PaymentManagerImpl implements PaymentManager {

    private final RestTemplate restTemplate;
    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;
    private final Random random = new Random();

    private int contador = 0;

    public PaymentManagerImpl(RestTemplate restTemplate, CircuitBreakerRegistry registry) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = registry.circuitBreaker("processPayment");
    }

    @PostConstruct
    public void init() { //Registra cuando cambia de estado
        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                        log.info("####### TRANSITION [{}] -> [{}]",
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()));
    }

    @Override
    @Retry(name = "processPayment")
    @CircuitBreaker(name = "processPayment", fallbackMethod = "fallbackPayment")
    public void processPayment() throws TimeoutException {
        contador++;
        log.info("CALL processPayment INIT, COUNT: {}", contador);

        if ((contador >= 5 && contador <= 8) || contador == 10) {
            // Fuerza error en llamadas 4 a 8
            if (contador == 10) contador = 0;
            log.warn("CALL processPayment ERROR, COUNT: {}", contador);
            throw new TimeoutException("Fallo forzado en intento " + contador);
        }
        //CUANDO ESTÁ ABIERTO NO SE LLAMA A restTemplate.getForEntity
        restTemplate.getForEntity("https://www.google.com", String.class);
        log.info("CALL processPayment SUCCESS, COUNT: {}", contador);
    }

    public void fallbackPayment(Throwable throwable) {
        log.info("CALL processPayment FALLBACK, COUNT: {}", contador);
        log.warn("Fallback triggered, CircuitBreaker state: {}, Reason: {}",
                circuitBreaker.getState(), throwable.getMessage());
    }
}
