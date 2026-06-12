package com.shopverse.web.controller;

import com.shopverse.infrastructure.elasticsearch.ProductDocument;
import com.shopverse.infrastructure.elasticsearch.ProductSearchService;
import com.shopverse.shared.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ch12-04: Resilience4j — circuit breaker wraps Elasticsearch calls.
 * If ES is down, fallback returns empty results (graceful degradation).
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final ProductSearchService searchService;

    public SearchController(ProductSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @CircuitBreaker(name = "productService", fallbackMethod = "searchFallback")
    @RateLimiter(name = "apiRateLimit")
    public ResponseEntity<ApiResponse<List<ProductDocument>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(searchService.search(q, page, size)));
    }

    @GetMapping("/autocomplete")
    @CircuitBreaker(name = "productService", fallbackMethod = "autocompleteFallback")
    public ResponseEntity<ApiResponse<List<String>>> autocomplete(@RequestParam String prefix) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(searchService.autocomplete(prefix)));
        } catch (Exception e) {
            return autocompleteFallback(prefix, e);
        }
    }

    // Fallback methods
    public ResponseEntity<ApiResponse<List<ProductDocument>>> searchFallback(String q, int page, int size, Throwable ex) {
        log.warn("Search circuit open, returning empty: {}", ex.getMessage());
        return ResponseEntity.ok(ApiResponse.ok(List.of(), "Search temporarily unavailable"));
    }

    public ResponseEntity<ApiResponse<List<String>>> autocompleteFallback(String prefix, Throwable ex) {
        return ResponseEntity.ok(ApiResponse.ok(List.of()));
    }
}
