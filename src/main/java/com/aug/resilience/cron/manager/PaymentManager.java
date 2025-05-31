package com.aug.resilience.cron.manager;

import java.util.concurrent.TimeoutException;

public interface PaymentManager {

    void processPayment() throws TimeoutException;

}
