package com.shopverse.infrastructure.jpa.repository;

import com.shopverse.infrastructure.jpa.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Ch04-04: Spring Data JPA repository — CRUD + derived + JPQL + native queries.
 * Ch05-03: Leverages partial index on active products.
 */
public interface JpaProductRepository extends JpaRepository<ProductEntity, Long> {

    List<ProductEntity> findByCategoryAndActiveTrue(String category);

    List<ProductEntity> findByActiveTrueOrderByCreatedAtDesc();

    // Ch04-04: JPQL named param query
    @Query("SELECT p FROM ProductEntity p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) AND p.active = true")
    List<ProductEntity> searchByName(@Param("q") String query);

    // Ch05-03: Native SQL — stock update with optimistic lock check
    @Modifying
    @Query(value = "UPDATE products SET stock_quantity = stock_quantity - :qty WHERE id = :id AND stock_quantity >= :qty",
           nativeQuery = true)
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    // Ch04-04: Projection — count per category
    @Query("SELECT p.category, COUNT(p) FROM ProductEntity p WHERE p.active = true GROUP BY p.category")
    List<Object[]> countByCategory();
}
// Note: this file needs JpaSpecificationExecutor added to the interface
// See JpaProductRepositoryWithSpec for the combined interface
