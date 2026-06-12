package com.shopverse.infrastructure.elasticsearch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ch08-01: Spring Data Elasticsearch repository.
 */
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    Page<ProductDocument> findByActiveTrueAndCategory(String category, Pageable pageable);

    @Query("""
        {
          "bool": {
            "must": [
              { "multi_match": {
                  "query": "?0",
                  "fields": ["name^3", "description", "category^2"],
                  "fuzziness": "AUTO"
              }}
            ],
            "filter": [
              { "term": { "active": true } },
              { "range": { "stock_quantity": { "gt": 0 } } }
            ]
          }
        }
        """)
    Page<ProductDocument> fullTextSearch(String query, Pageable pageable);
}
