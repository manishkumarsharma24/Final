package com.shopverse.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Ch13-09: JMH benchmark — object construction patterns.
 * Compares: new + setters, builder pattern, record construction.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class OrderBuilderBenchmark {

    @Benchmark
    public void buildWithRecord(Blackhole bh) {
        var item = new OrderItemRecord(1L, "Widget", 2, new BigDecimal("9.99"), "USD");
        bh.consume(item);
    }

    @Benchmark
    public void buildWithPojo(Blackhole bh) {
        OrderItemPojo item = new OrderItemPojo();
        item.setProductId(1L);
        item.setProductName("Widget");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("9.99"));
        bh.consume(item);
    }

    record OrderItemRecord(Long productId, String productName, int quantity,
                           BigDecimal unitPrice, String currency) {}

    static class OrderItemPojo {
        private Long productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        void setProductId(Long id)          { this.productId = id; }
        void setProductName(String name)    { this.productName = name; }
        void setQuantity(int qty)           { this.quantity = qty; }
        void setUnitPrice(BigDecimal price) { this.unitPrice = price; }
    }
}
