// common/src/main/java/com/acme/common/model/OrderCreated.java
package com.acme.common.model;

public record OrderCreated(
        String orderId,
        String customer,
        double amount,
        boolean forceFail,
        String correlationId,
        long createdAtEpochMs,
        int attempt
) {}