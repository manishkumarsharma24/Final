package com.shopverse.application.usecase.product;

import com.shopverse.application.command.UpdateProductCommand;
import com.shopverse.application.service.cache.CachedProductService;
import com.shopverse.domain.event.ProductEvent;
import com.shopverse.domain.exception.ProductNotFoundException;
import com.shopverse.domain.model.Product;
import com.shopverse.domain.port.EventPublisher;
import com.shopverse.domain.port.ProductRepository;
import com.shopverse.domain.vo.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Updates an existing product and keeps all Redis caches consistent.
 *
 * Cache behaviour (via CachedProductService.save()):
 *   @CachePut  → writes updated product to Redis: products::<id>
 *   @CacheEvict allEntries → clears all page/category list caches
 *
 * This prevents stale list results after an update.
 */
@Service
public class UpdateProductUseCase {

    private final ProductRepository productRepository;
    private final CachedProductService cachedProductService;
    private final EventPublisher eventPublisher;

    public UpdateProductUseCase(ProductRepository productRepository,
                                CachedProductService cachedProductService,
                                EventPublisher eventPublisher) {
        this.productRepository    = productRepository;
        this.cachedProductService = cachedProductService;
        this.eventPublisher       = eventPublisher;
    }

    @Transactional
    public Product execute(UpdateProductCommand cmd) {
        Product product = productRepository.findById(cmd.productId())
                .orElseThrow(() -> new ProductNotFoundException(cmd.productId()));

        // Apply field updates
        product.setName(cmd.name());
        product.setDescription(cmd.description());
        product.setPrice(new Money(cmd.price(), cmd.currency()));
        product.setCategory(cmd.category());

        // Stock: adjust via domain methods (no direct setter — domain enforces invariants)
        int currentStock = product.getStockQuantity();
        int newStock     = cmd.stockQuantity();
        if (newStock > currentStock) {
            product.replenishStock(newStock - currentStock);
        } else if (newStock < currentStock) {
            product.reduceStock(currentStock - newStock);
        }

        // @CachePut(products::<id>) + @CacheEvict(allEntries) — keeps Redis consistent
        Product saved = cachedProductService.save(product);

        // Notify ES to re-index the updated product (async, after commit)
        eventPublisher.publish(new ProductEvent.ProductUpdated(
                saved.getId(), "updated", Instant.now()));

        return saved;
    }
}
