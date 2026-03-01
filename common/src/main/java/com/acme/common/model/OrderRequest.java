// common/src/main/java/com/acme/common/model/OrderRequest.java
package com.acme.common.model;

public record OrderRequest(String orderId, String customer, double amount, boolean forceFail) {}