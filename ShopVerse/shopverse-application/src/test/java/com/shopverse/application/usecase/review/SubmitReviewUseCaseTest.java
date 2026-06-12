package com.shopverse.application.usecase.review;

import com.shopverse.domain.exception.CustomerNotFoundException;
import com.shopverse.domain.exception.ProductNotFoundException;
import com.shopverse.domain.model.Customer;
import com.shopverse.domain.model.Product;
import com.shopverse.domain.model.Review;
import com.shopverse.domain.port.CustomerRepository;
import com.shopverse.domain.port.ProductRepository;
import com.shopverse.domain.port.ReviewRepository;
import com.shopverse.domain.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmitReviewUseCase")
class SubmitReviewUseCaseTest {

    @Mock private ReviewRepository   reviewRepository;
    @Mock private ProductRepository  productRepository;
    @Mock private CustomerRepository customerRepository;

    @InjectMocks private SubmitReviewUseCase useCase;

    private Customer customer;
    private Product  product;

    @BeforeEach
    void setUp() {
        customer = new Customer(1L, "Alice", "Smith", "alice@example.com");
        product  = Product.builder()
                .id(10L).name("Widget")
                .price(new Money(BigDecimal.TEN, "USD"))
                .stockQuantity(100).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            return new Review("mongo-id-1", r.getProductId(), r.getCustomerId(),
                    r.getRating(), r.getTitle(), r.getBody(),
                    r.getTags(), 0, r.isVerified(), Instant.now());
        });
    }

    @Test
    @DisplayName("saves review and returns ReviewWithCustomer")
    void submit_review_success() {
        ReviewWithCustomer result = useCase.execute(
                10L, 1L, 5, "Great widget!", "Really loved it",
                List.of("quality", "fast"), true);

        assertNotNull(result);
        assertEquals(5, result.review().getRating());
        assertEquals("Great widget!", result.review().getTitle());
        assertEquals("Alice Smith", result.customer().getFullName());
        verify(reviewRepository).save(any());
    }

    @Test
    @DisplayName("throws ProductNotFoundException when product does not exist")
    void throws_when_product_not_found() {
        when(productRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class,
                () -> useCase.execute(10L, 1L, 4, "Good", "Works well", List.of(), false));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws CustomerNotFoundException when customer does not exist")
    void throws_when_customer_not_found() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(CustomerNotFoundException.class,
                () -> useCase.execute(10L, 1L, 4, "Good", "Works well", List.of(), false));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws IllegalArgumentException when rating < 1")
    void throws_when_rating_too_low() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(10L, 1L, 0, "Bad", "Terrible", List.of(), false));
    }

    @Test
    @DisplayName("throws IllegalArgumentException when rating > 5")
    void throws_when_rating_too_high() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(10L, 1L, 6, "Off chart", "Too good", List.of(), false));
    }
}
