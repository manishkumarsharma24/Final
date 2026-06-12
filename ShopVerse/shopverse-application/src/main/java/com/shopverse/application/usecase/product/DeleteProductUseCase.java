package com.shopverse.application.usecase.product;

import com.shopverse.application.service.cache.CachedProductService;
import com.shopverse.domain.event.ProductEvent;
import com.shopverse.domain.exception.ProductNotFoundException;
import com.shopverse.domain.port.EventPublisher;
import com.shopverse.domain.port.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DeleteProductUseCase {

    private final ProductRepository productRepository;
    private final CachedProductService cachedProductService;
    private final EventPublisher eventPublisher;

    public DeleteProductUseCase(ProductRepository productRepository,
                                CachedProductService cachedProductService,
                                EventPublisher eventPublisher) {
        this.productRepository    = productRepository;
        this.cachedProductService = cachedProductService;
        this.eventPublisher       = eventPublisher;
    }

    @Transactional
    public void execute(Long id) {
        productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productRepository.deleteById(id);
        // @CacheEvict — removes products::<id> and all category caches from Redis
        cachedProductService.evict(id);
        eventPublisher.publish(new ProductEvent.ProductUpdated(id, "deleted", Instant.now()));
    }
}
