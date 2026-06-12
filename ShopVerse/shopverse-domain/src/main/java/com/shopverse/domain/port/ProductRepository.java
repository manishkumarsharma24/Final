package com.shopverse.domain.port;

import com.shopverse.domain.model.Product;

import java.util.List;
import java.util.Optional;

/** Ch03-04: Product output port. */
public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(Long id);
    List<Product> findByCategory(String category);
    List<Product> findAll(int page, int size);
    List<Product> searchByName(String query);
    void deleteById(Long id);
}
