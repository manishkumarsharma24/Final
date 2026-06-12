package com.shopverse.infrastructure.elasticsearch;

import com.shopverse.domain.event.ProductEvent;
import com.shopverse.domain.model.Product;
import com.shopverse.domain.port.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;

/**
 * Ch08-02: Elasticsearch sync — listens to domain events, updates search index.
 * Async to avoid blocking the transaction that raised the event.
 */
@Component
public class ProductSyncService {

    private static final Logger log = LoggerFactory.getLogger(ProductSyncService.class);
    private final ProductSearchRepository searchRepo;
    private final ProductRepository productRepository;
    private final ElasticsearchOperations esOps;

    public ProductSyncService(ProductSearchRepository searchRepo,
                              ProductRepository productRepository,
                              ElasticsearchOperations esOps) {
        this.searchRepo        = searchRepo;
        this.productRepository = productRepository;
        this.esOps             = esOps;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductCreated(ProductEvent.ProductCreated event) {
        log.info("ES sync: indexing new product {}", event.productId());
        productRepository.findById(event.productId())
                .ifPresentOrElse(
                        this::indexProduct,
                        () -> log.warn("ES sync: product {} not found, skipping index", event.productId())
                );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductUpdated(ProductEvent.ProductUpdated event) {
        if ("deleted".equals(event.field())) {
            log.info("ES sync: removing deleted product {}", event.productId());
            searchRepo.deleteById(event.productId().toString());
        } else {
            log.info("ES sync: updating product {}", event.productId());
            productRepository.findById(event.productId())
                    .ifPresent(this::indexProduct);
        }
    }

    public void indexProduct(Product product) {
        ProductDocument doc = new ProductDocument();
        doc.setId(product.getId().toString());
        doc.setName(product.getName());
        doc.setDescription(product.getDescription());
        doc.setCategory(product.getCategory());
        doc.setPrice(product.getPrice().amount());
        doc.setStockQuantity(product.getStockQuantity());
        doc.setActive(product.isActive());
        doc.setSuggest(new Completion(new String[]{ product.getName() }));
        doc.setCreatedAt(Instant.now());
        searchRepo.save(doc);
        log.debug("Indexed product {} in Elasticsearch", product.getId());
    }

    public void deleteProduct(Long productId) {
        searchRepo.deleteById(productId.toString());
    }

    /** Bulk reindex — drops and recreates the ES index, then re-indexes all products from DB. */
    public int reindexAll() {
        // Drop and recreate index to ensure correct mappings (e.g. completion field for suggest)
        IndexOperations indexOps = esOps.indexOps(ProductDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
            log.info("ES: dropped existing products index");
        }
        indexOps.createWithMapping();
        log.info("ES: recreated products index with correct mappings");

        int page = 0, pageSize = 100, total = 0;
        List<Product> batch;
        do {
            batch = productRepository.findAll(page++, pageSize);
            batch.forEach(this::indexProduct);
            total += batch.size();
        } while (batch.size() == pageSize);
        log.info("Reindex complete — {} products indexed", total);
        return total;
    }
}
