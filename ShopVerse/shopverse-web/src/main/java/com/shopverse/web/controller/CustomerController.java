package com.shopverse.web.controller;

import com.shopverse.domain.exception.CustomerNotFoundException;
import com.shopverse.domain.model.Customer;
import com.shopverse.domain.port.CustomerRepository;
import com.shopverse.shared.ApiResponse;
import com.shopverse.web.dto.CustomerResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(@PathVariable Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
        return ResponseEntity.ok(ApiResponse.ok(CustomerResponse.from(c)));
    }
}
