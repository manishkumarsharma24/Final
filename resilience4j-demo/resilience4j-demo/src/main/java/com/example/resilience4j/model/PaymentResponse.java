package com.example.resilience4j.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the response from Payment Service.
 * Can be SUCCESS, PENDING (fallback), or ERROR.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String orderId;
    private String transactionId;
    private String status;       // SUCCESS, PENDING, ERROR, DECLINED
    private String message;
    private String source;       // PAYMENT_SERVICE or FALLBACK — tells us where response came from

    // -------------------------------------------------------
    // Static factory methods — clean way to create responses
    // -------------------------------------------------------

    public static PaymentResponse success(String orderId, String txnId) {
        return PaymentResponse.builder()
                .orderId(orderId)
                .transactionId(txnId)
                .status("SUCCESS")
                .message("Payment processed successfully")
                .source("PAYMENT_SERVICE")
                .build();
    }

    public static PaymentResponse pending(String orderId, String message) {
        // Fallback response — payment not confirmed but not failed either
        // Order saved, payment will be retried async
        return PaymentResponse.builder()
                .orderId(orderId)
                .transactionId("PENDING-" + System.currentTimeMillis())
                .status("PENDING")
                .message(message)
                .source("FALLBACK")
                .build();
    }

    public static PaymentResponse error(String orderId, String message) {
        return PaymentResponse.builder()
                .orderId(orderId)
                .transactionId(null)
                .status("ERROR")
                .message(message)
                .source("FALLBACK")
                .build();
    }
}
