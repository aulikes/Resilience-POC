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
        AtomicInteger cont = new AtomicInteger();
        int DELAY_INIT_SEG = 10;
        int PERIOD_SEG = 10;

        futureTask = executorService.scheduleAtFixedRate(() -> {
            try {
                log.info("**************************************************");
                cont.getAndIncrement();
                log.info("Ejecutando prueba peridodo: {}", (PERIOD_SEG*(cont.get())));
                paymentService.processPayment();
                log.info("--------------------------------------------------");
            } catch (Exception e) {
                log.error("Error en ejecución programada: {}", e.getMessage(), e);
            }
        }, DELAY_INIT_SEG, PERIOD_SEG, TimeUnit.SECONDS); // 10s delay inicial, luego cada 15s

        // Programar detención después de 2 minutos
        executorService.schedule(() -> {
            futureTask.cancel(false);
            log.warn("Tarea programada detenida tras 3 minutos.");
            executorService.shutdown();
        }, 3, TimeUnit.MINUTES);
    }
}
