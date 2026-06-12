package com.shopverse.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record NotificationRequest(
        @NotNull Long orderId,
        @NotNull @Email String customerEmail,
        @NotBlank String type,         // ORDER_CONFIRMED | ORDER_SHIPPED | ORDER_DELIVERED | ORDER_CANCELLED | PAYMENT_SUCCESS | PAYMENT_FAILED
        Long customerId,
        BigDecimal amount,
        String trackingNumber,
        String reason,
        String paymentMethod) {
}
