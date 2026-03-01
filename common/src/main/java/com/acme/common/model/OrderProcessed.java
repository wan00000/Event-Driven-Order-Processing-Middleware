// common/src/main/java/com/acme/common/model/OrderProcessed.java
package com.acme.common.model;

public record OrderProcessed(
        String orderId,
        String status,
        String correlationId,
        long processedAtEpochMs
) {}