package com.shopverse.web.controller;

import com.shopverse.application.command.UpdateProductCommand;
import com.shopverse.application.query.SearchProductsQuery;
import com.shopverse.application.service.cache.CachedProductService;
import com.shopverse.application.usecase.product.CreateProductUseCase;
import com.shopverse.application.usecase.product.DeleteProductUseCase;
import com.shopverse.application.usecase.product.SearchProductsUseCase;
import com.shopverse.application.usecase.product.UpdateProductUseCase;
import com.shopverse.domain.model.Product;
import com.shopverse.infrastructure.elasticsearch.ProductSyncService;
import com.shopverse.shared.ApiResponse;
import com.shopverse.shared.PagedResponse;
import com.shopverse.web.dto.ProductRequest;
import com.shopverse.web.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Ch07-01: REST controller — Product resource CRUD.
 * Ch07-06: @PreAuthorize — method-level security.
 * Ch10-03: i18n via Accept-Language (handled by MessageSource).
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final SearchProductsUseCase searchProductsUseCase;
    private final ProductSyncService productSyncService;
    private final CachedProductService cachedProductService;

    public ProductController(CreateProductUseCase createProductUseCase,
                             UpdateProductUseCase updateProductUseCase,
                             DeleteProductUseCase deleteProductUseCase,
                             SearchProductsUseCase searchProductsUseCase,
                             ProductSyncService productSyncService,
                             CachedProductService cachedProductService) {
        this.createProductUseCase  = createProductUseCase;
        this.updateProductUseCase  = updateProductUseCase;
        this.deleteProductUseCase  = deleteProductUseCase;
        this.searchProductsUseCase = searchProductsUseCase;
        this.productSyncService    = productSyncService;
        this.cachedProductService  = cachedProductService;
    }

    /**
     * GET /api/products/{id}
     * Redis flow:
     *   1st call → @Cacheable miss → PostgreSQL SELECT → stored in Redis (products::<id>, TTL 30 min)
     *   2nd call → @Cacheable hit  → served from Redis, PostgreSQL never called
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable Long id) {
        Product product = cachedProductService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(ProductResponse.from(product)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Product> products = searchProductsUseCase.execute(
                new SearchProductsQuery(keyword, category, page, size));
        List<ProductResponse> dtos = products.stream().map(ProductResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(dtos, page, size, dtos.size())));
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> reindex() {
        int count = productSyncService.reindexAll();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("indexed", count), "Reindex complete"));
    }

    /**
     * PUT /api/products/{id}
     * Redis flow:
     *   → @CachePut  updates products::<id> with new data
     *   → @CacheEvict allEntries clears all page/category list caches
     *   → next GET /api/products or GET /api/products?category=X will be fresh from PostgreSQL
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest req) {
        Product updated = updateProductUseCase.execute(new UpdateProductCommand(
                id, req.name(), req.description(), req.price(), req.currency(),
                req.category(), req.stockQuantity()));
        return ResponseEntity.ok(ApiResponse.ok(ProductResponse.from(updated), "Product updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        deleteProductUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Product deleted successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductRequest req) {
        Product created = createProductUseCase.execute(new UpdateProductCommand(
                null, req.name(), req.description(), req.price(), req.currency(), req.category(), req.stockQuantity()));
        return ResponseEntity.status(201).body(ApiResponse.ok(ProductResponse.from(created)));
    }
}
