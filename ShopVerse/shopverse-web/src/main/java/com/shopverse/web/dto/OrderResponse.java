package com.shopverse.web.dto;

import com.shopverse.domain.model.Order;
import com.shopverse.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        OrderStatus status,
        BigDecimal total,
        String currency,
        List<OrderItemResponse> items,
        Instant createdAt
) {
    public record OrderItemResponse(Long productId, String productName, int quantity, BigDecimal unitPrice) {}

    public static OrderResponse from(Order o) {
        return new OrderResponse(
                o.getId(), o.getCustomerId(), o.getStatus(),
                o.total().amount(), o.total().currency(),
                o.getItems().stream().map(i ->
                    new OrderItemResponse(i.getProductId(), i.getProductName(),
                                         i.getQuantity(), i.getUnitPrice().amount())
                ).toList(),
                o.getCreatedAt()
        );
    }
}
