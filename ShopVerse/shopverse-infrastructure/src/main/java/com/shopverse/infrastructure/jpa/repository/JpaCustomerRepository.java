package com.shopverse.infrastructure.jpa.repository;

import com.shopverse.infrastructure.jpa.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/** Ch04-04: Customer JPA repository. */
public interface JpaCustomerRepository extends JpaRepository<CustomerEntity, Long> {

    Optional<CustomerEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT c FROM CustomerEntity c WHERE c.tier = :tier AND c.active = true")
    java.util.List<CustomerEntity> findByTier(@org.springframework.data.repository.query.Param("tier") String tier);
}
