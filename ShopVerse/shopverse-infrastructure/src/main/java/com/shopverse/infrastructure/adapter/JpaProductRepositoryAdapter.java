package com.shopverse.infrastructure.adapter;

import com.shopverse.domain.exception.ProductNotFoundException;
import com.shopverse.domain.model.Product;
import com.shopverse.domain.port.ProductRepository;
import com.shopverse.infrastructure.jpa.entity.ProductEntity;
import com.shopverse.infrastructure.jpa.mapper.ProductMapper;
import com.shopverse.infrastructure.jpa.repository.JpaProductRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Ch03-04: Hexagonal adapter — implements domain port using Spring Data JPA.
 * Domain layer sees only the port interface; Spring annotations stay here.
 */
@Repository
public class JpaProductRepositoryAdapter implements ProductRepository {

    private final JpaProductRepository jpaRepo;
    private final ProductMapper mapper;

    public JpaProductRepositoryAdapter(JpaProductRepository jpaRepo, ProductMapper mapper) {
        this.jpaRepo = jpaRepo;
        this.mapper  = mapper;
    }

    @Override
    public Product save(Product product) {
        if (product.getId() != null) {
            // UPDATE path: load the managed entity so Hibernate's @Version is preserved.
            // Constructing a fresh ProductEntity(id=X, version=null) causes:
            //   "Detached entity with generated id has an uninitialized version value"
            // because @Version requires the field to be non-null for merges.
            ProductEntity existing = jpaRepo.findById(product.getId())
                    .orElseThrow(() -> new ProductNotFoundException(product.getId()));
            mapper.updateEntity(product, existing);   // updates all mutable fields in-place
            return mapper.toDomain(jpaRepo.save(existing));
        }
        // INSERT path: id is null, Hibernate generates it and initialises version = 0
        ProductEntity entity = mapper.toEntity(product);
        return mapper.toDomain(jpaRepo.save(entity));
    }

    @Override
    public Optional<Product> findById(Long id) {
        return jpaRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Product> findByCategory(String category) {
        return jpaRepo.findByCategoryAndActiveTrue(category)
                      .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Product> findAll(int page, int size) {
        return jpaRepo.findByActiveTrueOrderByCreatedAtDesc()
                      .stream().skip((long) page * size).limit(size)
                      .map(mapper::toDomain).toList();
    }

    @Override
    public List<Product> searchByName(String query) {
        return jpaRepo.searchByName(query).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }
}
