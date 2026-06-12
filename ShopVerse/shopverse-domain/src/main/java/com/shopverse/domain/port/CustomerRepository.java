package com.shopverse.domain.port;

import com.shopverse.domain.model.Customer;

import java.util.Optional;

/** Ch03-04: Customer output port. */
public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(Long id);
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);
    void deleteById(Long id);
}
