package com.shopverse.application.usecase.review;

import com.shopverse.domain.model.Customer;
import com.shopverse.domain.model.Review;
import com.shopverse.domain.port.CustomerRepository;
import com.shopverse.domain.port.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ch06-05: Fetch reviews for a product from MongoDB, enriched with customer data from PostgreSQL.
 */
@Service
public class GetReviewsUseCase {

    private final ReviewRepository   reviewRepository;
    private final CustomerRepository customerRepository;

    public GetReviewsUseCase(ReviewRepository reviewRepository,
                             CustomerRepository customerRepository) {
        this.reviewRepository   = reviewRepository;
        this.customerRepository = customerRepository;
    }

    public List<ReviewWithCustomer> getByProduct(Long productId) {
        return enrich(reviewRepository.findByProductId(productId));
    }

    public List<ReviewWithCustomer> getByProductWithMinRating(Long productId, int minRating) {
        return enrich(reviewRepository.findByProductIdAndMinRating(productId, minRating));
    }

    public ReviewRepository.RatingStats getRatingStats(Long productId) {
        return reviewRepository.aggregateRating(productId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Enriches a list of reviews with their authors.
     * Loads each unique customerId once from PostgreSQL (no N+1),
     * then pairs each review with its Customer.
     * Reviews whose customer is not found are still returned with a null customer.
     */
    private List<ReviewWithCustomer> enrich(List<Review> reviews) {
        // Collect unique customer IDs
        Map<Long, Customer> customerMap = reviews.stream()
                .map(Review::getCustomerId)
                .distinct()
                .map(id -> customerRepository.findById(id).orElse(null))
                .filter(c -> c != null)
                .collect(Collectors.toMap(Customer::getId, c -> c));

        return reviews.stream()
                .map(r -> new ReviewWithCustomer(r, customerMap.get(r.getCustomerId())))
                .toList();
    }
}
