package com.aug.resilience.cron.scheduled;

import com.aug.resilience.cron.manager.PaymentManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTask {

    private final PaymentManager paymentService;
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> futureTask;

    @PostConstruct
    public void iniciarEjecucionProgramada() {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger cont = new AtomicInteger(0);
        int DELAY_INIT_SEG = 20;
        int PERIOD_SEG = 10;
        int FINAL_TEST_SEG = 720;

        futureTask = executorService.scheduleAtFixedRate(() -> {
            try {
                log.info("**************************************************");
                cont.getAndIncrement();
                log.info("Running Test NUMBER: {}", (cont.get()));
                paymentService.processPayment();
                log.info("--------------------------------------------------");
            } catch (Exception e) {
                log.error("Error en ejecución programada: {}", e.getMessage(), e);
            }
        }, DELAY_INIT_SEG, PERIOD_SEG, TimeUnit.SECONDS); // DELAY_INIT_SEG delay inicial, luego cada PERIOD_SEG

        // Programar detención después de 2 minutos
        executorService.schedule(() -> {
            futureTask.cancel(false);
            log.warn("Tarea programada detenida tras {} segundos.", FINAL_TEST_SEG);
            executorService.shutdown();
        }, FINAL_TEST_SEG, TimeUnit.SECONDS);
    }
}
