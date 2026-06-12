package com.shopverse.infrastructure.jpa.repository;

import com.shopverse.infrastructure.jpa.entity.ProductEntity;
import com.shopverse.infrastructure.jpa.projection.ProductSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Ch04-08: Extended repository — supports both CRUD and Specification API.
 */
public interface JpaProductRepositoryWithSpec
        extends JpaRepository<ProductEntity, Long>,
                JpaSpecificationExecutor<ProductEntity> {

    List<ProductSummary> findProjectedByActiveTrue();
}
