package com.aug.resilience.cron.manager;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

@Component
@Log4j2
public class PaymentManagerImpl implements PaymentManager {

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    private int contador = 1;

    public PaymentManagerImpl(RestTemplate restTemplate,
                              CircuitBreakerRegistry circuitBreakerRegistry,
                              RetryRegistry retryRegistry) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("processPayment");
        this.retry = retryRegistry.retry("processPaymentRetry");
    }

    /*
     1. Cuando contador%modular(1,2,3,4,5) el circuito estará CLOSED
     2. Cuando contador%modular(6,7,8) el circuito estará CLOSED aunque estas peticiones van a fallar
     3. Debido a que ya fallaron más o igual al porcentaje establecido en failureRateThreshold, el circuito pasará a OPEN
     5. Con el Circuito en estado OPEN, no se llama al método processPayment, sino que automáticamente pasará a fallbackPayment
     4. Cuando se va a realizar la petición Número 9 el circuito ya está en OPEN.
     6. El circuito pasa de OPEN a HALF-OPEN debido al tiempo establecido en waitDurationInOpenState
     7. waitDurationInOpenState empieza a contar desde que el Circuito pasó a estado OPEN, en el punto 3, después de la 8 llamada.
     8. El tiempo establecido en waitDurationInOpenState está a 35 seg, por tanto para LAS PETICIONES 9, 10 Y 11 pasan directo al método fallbackPayment
     9. La petición 12 se realiza con el circuito en estado HALF-OPEN, por eso entra al método processPayment, y se realiza bien.
     10. La petición 13 se realiza con el circuito en estado HALF-OPEN, por eso entra al método processPayment, y Lanza un error.
     11. La petición 14 se realiza con el circuito en estado HALF-OPEN, por eso entra al método processPayment, y se realiza bien.
     12. En este caso no se realizan con éxito el número de peticiones establecidas en permittedNumberOfCallsInHalfOpenState
     13. En consecuencia, el circuito pasa de HALF-OPEN a OPEN.
     14. El circuito pasa de OPEN a HALF-OPEN debido al tiempo establecido en waitDurationInOpenState
     15. El tiempo establecido en waitDurationInOpenState está a 35 seg, por tanto para LAS PETICIONES 15, 16 Y 17 pasan directo al método fallbackPayment
     16. Las peticiones 18, 19 y 20 se realizan con el circuito en estado HALF-OPEN, por eso entra al método processPayment, y en este caso dan OK.
     17. En este punto ya se han cumplido las peticiones mínimas establecidas en permittedNumberOfCallsInHalfOpenState
     18. Por tanto, ya el circuito pasa de HALF-OPEN -> CLOSED
     */
    @Override
    public void processPayment() {
        // Lógica principal encapsulada
        Supplier<Void> coreLogic = () -> {
            log.info("CALL processPayment INIT, COUNT: {}, CIRCUIT BREAKER STATE: {}", contador, circuitBreaker.getState());

            int modular = 20;
            if (contador < 15 && contador > 0 &&
                    (contador % modular == 6 || contador % modular == 7 || contador % modular == 8 || contador % modular == 13)) {
                throw new RuntimeException("Simulated failure, COUNT: " + contador);
            }

            restTemplate.getForEntity("https://www.google.com", String.class);
            log.info("CALL processPayment SUCCESS, COUNT: {}", contador);
            contador++;
            return null;
        };

        // AQUÍ EL RETRY ENCAPSULA AL CIRCUIT-BREAKER (NO debería ser así)
//        Supplier<Void> circuitBreakerWrapped = CircuitBreaker.decorateSupplier(circuitBreaker, coreLogic);
//        Supplier<Void> retryWrapped = Retry.decorateSupplier(retry, circuitBreakerWrapped);

        // Retry envuelve la lógica interna
        Supplier<Void> retryWrapped = Retry.decorateSupplier(retry, coreLogic);
        // CircuitBreaker envuelve el Retry como una unidad
        Supplier<Void> circuitBreakerWrapped = CircuitBreaker.decorateSupplier(circuitBreaker, retryWrapped);

        // Ejecutar con fallback en caso de excepción global
        try {
//            retryWrapped.get();
            circuitBreakerWrapped.get();
        } catch (Exception e) {
            fallbackPayment(e);
        }
    }


    /*
        Método que se ejecuta cuando ocurre un error en processPayment.
        Cuando el Circuito está ABIERTO, no se llama ni a processPayment
     */
    public void fallbackPayment(Throwable throwable) {
        log.error("CALL processPayment FALLBACK, COUNT: {}, CIRCUIT BREAKER ESTATE: {}", contador, circuitBreaker.getState());
//        log.error("Fallback triggered, CircuitBreaker state: {}, Reason: {}",
//                circuitBreaker.getState(), throwable.getMessage());
        contador++;
    }
}
