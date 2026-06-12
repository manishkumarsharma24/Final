package com.shopverse.application.usecase.customer;

import com.shopverse.domain.exception.ShopVerseException;
import com.shopverse.domain.model.Customer;
import com.shopverse.domain.model.ErrorCode;
import com.shopverse.domain.port.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterCustomerUseCase {

    private final CustomerRepository customerRepository;

    public RegisterCustomerUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Customer execute(String firstName, String lastName, String email) {
        if (customerRepository.existsByEmail(email)) {
            throw new ShopVerseException(ErrorCode.CUSTOMER_ALREADY_EXISTS,
                    "Customer already exists with email: " + email);
        }
        return customerRepository.save(new Customer(null, firstName, lastName, email));
    }
}
