package com.shopverse.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Ch13-09: JMH micro-benchmark — product search and filtering performance.
 * Measures: list iteration, stream filter, string matching.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class ProductSearchBenchmark {

    @Param({"100", "1000", "10000"})
    private int listSize;

    private List<FakeProduct> products;

    @Setup(Level.Trial)
    public void setup() {
        products = new ArrayList<>(listSize);
        for (int i = 0; i < listSize; i++) {
            products.add(new FakeProduct(
                (long) i,
                "Product " + i,
                i % 5 == 0 ? "electronics" : "clothing",
                BigDecimal.valueOf(10 + i * 0.5)
            ));
        }
    }

    @Benchmark
    public void filterByCategory_stream(Blackhole bh) {
        List<FakeProduct> result = products.stream()
            .filter(p -> "electronics".equals(p.category()))
            .collect(Collectors.toList());
        bh.consume(result);
    }

    @Benchmark
    public void filterByCategory_forLoop(Blackhole bh) {
        List<FakeProduct> result = new ArrayList<>();
        for (FakeProduct p : products) {
            if ("electronics".equals(p.category())) result.add(p);
        }
        bh.consume(result);
    }

    @Benchmark
    public void nameSearch_contains(Blackhole bh) {
        List<FakeProduct> result = products.stream()
            .filter(p -> p.name().toLowerCase().contains("product 5"))
            .collect(Collectors.toList());
        bh.consume(result);
    }

    // Simple value record for benchmarking
    record FakeProduct(Long id, String name, String category, BigDecimal price) {}
}
