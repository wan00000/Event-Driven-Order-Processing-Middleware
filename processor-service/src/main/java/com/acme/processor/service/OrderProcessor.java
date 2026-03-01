// processor-service/src/main/java/com/acme/processor/service/OrderProcessor.java
package com.acme.processor.service;

import com.acme.common.model.OrderCreated;
import org.eclipse.microprofile.faulttolerance.Retry;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderProcessor {

    // IMPORTANT: must be called via CDI proxy (i.e., injected into another bean)
    @Retry(maxRetries = 2, delay = 1000) // total attempts = 1 + 2 retries = 3
    public void process(OrderCreated order) {
        if (order.forceFail()) {
            throw new RuntimeException("Forced failure for testing (orderId=" + order.orderId() + ")");
        }

        // Simulate business logic / downstream call
        // You can add random failures here later
    }
}