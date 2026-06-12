package com.shopverse.infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ch08-04: Elasticsearch service — full-text search + autocomplete suggestions.
 * Uses native ElasticsearchClient for completion suggester.
 */
@Service
public class ProductSearchService {

    private final ProductSearchRepository searchRepo;
    private final ElasticsearchClient esClient;

    public ProductSearchService(ProductSearchRepository searchRepo,
                                ElasticsearchClient esClient) {
        this.searchRepo = searchRepo;
        this.esClient   = esClient;
    }

    public List<ProductDocument> search(String query, int page, int size) {
        return searchRepo.fullTextSearch(query,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "avg_rating")))
            .getContent();
    }

    /** Ch08-04: Completion suggester — returns up to 5 autocomplete suggestions. */
    public List<String> autocomplete(String prefix) throws IOException {
        SearchResponse<ProductDocument> response = esClient.search(
            s -> s.index("products")
                  .suggest(sg -> sg
                      .suggesters("product-suggest",
                          sug -> sug.prefix(prefix)
                                    .completion(c -> c.field("suggest").size(5))
                      )
                  ),
            ProductDocument.class
        );
        return response.suggest()
            .getOrDefault("product-suggest", List.of())
            .stream()
            .flatMap(s -> s.completion().options().stream())
            .map(CompletionSuggestOption::text)
            .collect(Collectors.toList());
    }
}
