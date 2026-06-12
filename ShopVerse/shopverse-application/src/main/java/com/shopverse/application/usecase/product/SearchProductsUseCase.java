package com.shopverse.application.usecase.product;

import com.shopverse.application.query.SearchProductsQuery;
import com.shopverse.application.service.cache.CachedProductService;
import com.shopverse.domain.model.Product;
import com.shopverse.domain.port.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SearchProductsUseCase {

    private final ProductRepository productRepository;
    private final CachedProductService cachedProductService;

    public SearchProductsUseCase(ProductRepository productRepository,
                                 CachedProductService cachedProductService) {
        this.productRepository    = productRepository;
        this.cachedProductService = cachedProductService;
    }

    @Transactional(readOnly = true)
    public List<Product> execute(SearchProductsQuery query) {
        if (query.keyword() != null && !query.keyword().isBlank()) {
            // Keyword search — bypass cache, always hits PostgreSQL (dynamic query)
            return productRepository.searchByName(query.keyword());
        }
        if (query.category() != null && !query.category().isBlank()) {
            // @Cacheable — first call hits PostgreSQL, subsequent calls served from Redis
            // Redis key: "products::category:<category>", TTL 30 min
            return cachedProductService.findByCategory(query.category());
        }
        // @Cacheable — key: "products::all:page:<n>:size:<n>", TTL 30 min
        return cachedProductService.findAll(query.page(), query.size());
    }
}
