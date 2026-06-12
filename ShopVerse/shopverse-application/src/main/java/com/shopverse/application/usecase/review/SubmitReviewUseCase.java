package com.shopverse.application.usecase.review;

import com.shopverse.domain.exception.CustomerNotFoundException;
import com.shopverse.domain.exception.ProductNotFoundException;
import com.shopverse.domain.model.Customer;
import com.shopverse.domain.model.Review;
import com.shopverse.domain.port.CustomerRepository;
import com.shopverse.domain.port.ProductRepository;
import com.shopverse.domain.port.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ch06-05: Submit a product review.
 * Validates both product (PostgreSQL) and customer (PostgreSQL) exist,
 * then saves the review document to MongoDB via the ReviewRepository port.
 * Returns the saved review paired with the author's Customer data.
 */
@Service
public class SubmitReviewUseCase {

    private final ReviewRepository   reviewRepository;
    private final ProductRepository  productRepository;
    private final CustomerRepository customerRepository;

    public SubmitReviewUseCase(ReviewRepository reviewRepository,
                               ProductRepository productRepository,
                               CustomerRepository customerRepository) {
        this.reviewRepository   = reviewRepository;
        this.productRepository  = productRepository;
        this.customerRepository = customerRepository;
    }

    public ReviewWithCustomer execute(Long productId, Long customerId,
                                      int rating, String title, String body,
                                      List<String> tags, boolean verified) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Review review = new Review();
        review.setProductId(productId);
        review.setCustomerId(customerId);
        review.setRating(rating);
        review.setTitle(title);
        review.setBody(body);
        review.setTags(tags);
        review.setVerified(verified);

        Review saved = reviewRepository.save(review);
        return new ReviewWithCustomer(saved, customer);
    }
}
