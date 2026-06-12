package com.shopverse.application.service.cache;

import com.shopverse.domain.exception.ProductNotFoundException;
import com.shopverse.domain.model.Product;
import com.shopverse.domain.port.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ch11-05: Spring Cache abstraction — Redis-backed per-method caching.
 * @Cacheable  — cache on first call, serve from Redis on subsequent calls.
 * @CachePut   — always execute + update cache (used for writes).
 * @CacheEvict — invalidate cache on delete/deactivate.
 */
@Service
public class CachedProductService {

    private final ProductRepository productRepository;

    public CachedProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // ── "products" cache: individual lookups keyed by id ─────────────────────
    // Keys: products::1, products::2, ...
    // Only evicted when THAT specific product is updated or deleted.
    // Creating product-2 never touches product-1's cache entry.

    @Cacheable(cacheNames = "products", key = "#id")
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    // ── "products-list" cache: collection results ─────────────────────────────
    // Keys: products-list::category:electronics, products-list::all:page:0:size:20, ...
    // Cleared whenever any product is created, updated, or deleted — because any
    // product change can affect the contents of a list or page.

    @Cacheable(cacheNames = "products-list", key = "'category:' + #category")
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Cacheable(cacheNames = "products-list", key = "'all:page:' + #page + ':size:' + #size")
    public List<Product> findAll(int page, int size) {
        return productRepository.findAll(page, size);
    }

    // ── save (create or update) ───────────────────────────────────────────────
    // Two independent caches, no conflict — ordering does not matter:
    //   @CachePut  → writes products::<id>   (individual cache, only this product)
    //   @CacheEvict → clears products-list::* (list cache, all collection keys)
    //
    // Creating product-2 no longer evicts product-1:
    //   products::1 stays warm  ✓
    //   products::2 written     ✓
    //   products-list::*        cleared so next list/page call is fresh ✓
    @Caching(
        put   = { @CachePut(cacheNames = "products", key = "#result.id") },
        evict = { @CacheEvict(cacheNames = "products-list", allEntries = true) }
    )
    public Product save(Product product) {
        return productRepository.save(product);
    }

    // ── delete ────────────────────────────────────────────────────────────────
    // Remove the specific product from "products" and clear all list caches.
    @Caching(evict = {
        @CacheEvict(cacheNames = "products",      key = "#id"),
        @CacheEvict(cacheNames = "products-list", allEntries = true)
    })
    public void evict(Long id) {
        // products::<id> removed; products-list::* cleared
    }
}
