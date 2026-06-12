package com.shopverse.application.usecase.review;

import com.shopverse.domain.model.Customer;
import com.shopverse.domain.model.Review;
import com.shopverse.domain.port.CustomerRepository;
import com.shopverse.domain.port.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetReviewsUseCase")
class GetReviewsUseCaseTest {

    @Mock private ReviewRepository   reviewRepository;
    @Mock private CustomerRepository customerRepository;

    @InjectMocks private GetReviewsUseCase useCase;

    private Review review1;
    private Review review2;

    @BeforeEach
    void setUp() {
        review1 = new Review("id-1", 10L, 1L, 5, "Excellent", "Perfect",
                List.of("quality"), 3, true, Instant.now());
        review2 = new Review("id-2", 10L, 2L, 4, "Good", "Pretty good",
                List.of("value"), 1, false, Instant.now());
    }

    @Test
    @DisplayName("returns reviews enriched with customer data")
    void get_reviews_for_product() {
        when(reviewRepository.findByProductId(10L)).thenReturn(List.of(review1, review2));
        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(new Customer(1L, "Alice", "Smith", "alice@example.com")));
        when(customerRepository.findById(2L))
                .thenReturn(Optional.of(new Customer(2L, "Bob", "Jones", "bob@example.com")));

        List<ReviewWithCustomer> results = useCase.getForProduct(10L, null);

        assertEquals(2, results.size());
        assertEquals("Alice Smith", results.get(0).customer().getFullName());
        assertEquals("Bob Jones",   results.get(1).customer().getFullName());
    }

    @Test
    @DisplayName("returns only reviews meeting minimum rating filter")
    void get_reviews_with_min_rating() {
        when(reviewRepository.findByProductIdAndMinRating(10L, 5))
                .thenReturn(List.of(review1));
        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(new Customer(1L, "Alice", "Smith", "alice@example.com")));

        List<ReviewWithCustomer> results = useCase.getForProduct(10L, 5);

        assertEquals(1, results.size());
        assertEquals(5, results.get(0).review().getRating());
    }

    @Test
    @DisplayName("returns empty list when product has no reviews")
    void get_reviews_empty() {
        when(reviewRepository.findByProductId(99L)).thenReturn(List.of());
        List<ReviewWithCustomer> results = useCase.getForProduct(99L, null);
        assertTrue(results.isEmpty());
        verify(customerRepository, never()).findById(any());
    }

    @Test
    @DisplayName("loads each unique customer only once — no N+1 queries")
    void no_n_plus_1_for_customer_loading() {
        // Two reviews from the same customer
        Review review3 = new Review("id-3", 10L, 1L, 3, "OK", "Decent",
                List.of(), 0, false, Instant.now());
        when(reviewRepository.findByProductId(10L)).thenReturn(List.of(review1, review3));
        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(new Customer(1L, "Alice", "Smith", "alice@example.com")));

        useCase.getForProduct(10L, null);

        // Only ONE customer lookup despite TWO reviews from same customer
        verify(customerRepository, times(1)).findById(1L);
    }
}
