package com.shopverse.application.usecase.product;

import com.shopverse.application.command.UpdateProductCommand;
import com.shopverse.application.service.cache.CachedProductService;
import com.shopverse.domain.event.ProductEvent;
import com.shopverse.domain.model.Product;
import com.shopverse.domain.port.EventPublisher;
import com.shopverse.domain.vo.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CreateProductUseCase {

    private final CachedProductService cachedProductService;
    private final EventPublisher eventPublisher;

    public CreateProductUseCase(CachedProductService cachedProductService,
                                EventPublisher eventPublisher) {
        this.cachedProductService = cachedProductService;
        this.eventPublisher       = eventPublisher;
    }

    @Transactional
    public Product execute(UpdateProductCommand cmd) {
        Product product = new Product.Builder()
                .name(cmd.name())
                .description(cmd.description())
                .price(new Money(cmd.price(), cmd.currency()))
                .category(cmd.category())
                .stockQuantity(cmd.stockQuantity())
                .build();
        // @CachePut — saves to PostgreSQL AND writes to Redis cache (products::<id>, TTL 30 min)
        Product saved = cachedProductService.save(product);
        eventPublisher.publish(new ProductEvent.ProductCreated(
                saved.getId(), saved.getName(), Instant.now()));
        return saved;
    }
}
