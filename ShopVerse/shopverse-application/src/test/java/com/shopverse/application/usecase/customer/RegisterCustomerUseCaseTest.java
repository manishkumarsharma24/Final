package com.shopverse.application.usecase.customer;

import com.shopverse.domain.exception.ShopVerseException;
import com.shopverse.domain.model.Customer;
import com.shopverse.domain.port.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterCustomerUseCase")
class RegisterCustomerUseCaseTest {

    @Mock private CustomerRepository customerRepository;
    @InjectMocks private RegisterCustomerUseCase useCase;

    @Test
    @DisplayName("registers a new customer successfully")
    void registers_new_customer() {
        when(customerRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(customerRepository.save(any())).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            return new Customer(1L, c.getFirstName(), c.getLastName(), c.getEmail());
        });

        Customer result = useCase.execute("Bob", "Jones", "bob@example.com");

        assertNotNull(result);
        assertEquals("Bob", result.getFirstName());
        assertEquals("bob@example.com", result.getEmail());
        verify(customerRepository).save(any());
    }

    @Test
    @DisplayName("throws ShopVerseException when email is already registered")
    void throws_when_email_exists() {
        when(customerRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThrows(ShopVerseException.class,
                () -> useCase.execute("Bob", "Jones", "bob@example.com"));
        verify(customerRepository, never()).save(any());
    }
}
