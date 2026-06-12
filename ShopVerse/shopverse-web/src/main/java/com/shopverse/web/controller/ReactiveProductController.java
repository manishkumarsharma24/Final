package com.shopverse.web.controller;

import com.shopverse.application.service.cache.CachedProductService;
import com.shopverse.domain.model.Product;
import com.shopverse.web.dto.ProductResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * Ch11-02: WebFlux reactive endpoint — non-blocking product stream.
 * Uses Mono/Flux with blocking-to-reactive bridge for JPA calls.
 * Ch11-03: Runs blocking JPA on boundedElastic scheduler (virtual threads).
 */
@RestController
@RequestMapping("/api/reactive/products")
public class ReactiveProductController {

    private final CachedProductService productService;

    public ReactiveProductController(CachedProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public Mono<ProductResponse> getProduct(@PathVariable Long id) {
        return Mono.fromCallable(() -> productService.findById(id))
                   .subscribeOn(Schedulers.boundedElastic())
                   .map(ProductResponse::from);
    }

    /** Ch11-02: SSE stream — product catalog as Server-Sent Events. */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ProductResponse> streamProducts(
            @RequestParam(defaultValue = "electronics") String category) {
        return Flux.interval(Duration.ofSeconds(1))
                   .flatMap(tick -> Mono.fromCallable(
                       () -> productService.findByCategory(category))
                       .subscribeOn(Schedulers.boundedElastic()))
                   .flatMapIterable(list -> list)
                   .map(ProductResponse::from)
                   .take(50);  // max 50 items
    }
}
