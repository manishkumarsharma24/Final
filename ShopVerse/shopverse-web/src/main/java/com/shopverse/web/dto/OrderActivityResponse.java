package com.shopverse.web.dto;

import com.shopverse.domain.model.OrderActivity;

import java.time.Instant;
import java.util.UUID;

/** Response DTO for a single order activity event. */
public record OrderActivityResponse(
        Long   customerId,
        Long   orderId,
        String eventType,
        String details,
        Instant eventTime,
        UUID   eventId
) {
    public static OrderActivityResponse from(OrderActivity a) {
        return new OrderActivityResponse(
                a.getCustomerId(),
                a.getOrderId(),
                a.getEventType(),
                a.getDetails(),
                a.getEventTime(),
                a.getEventId()
        );
    }
}
