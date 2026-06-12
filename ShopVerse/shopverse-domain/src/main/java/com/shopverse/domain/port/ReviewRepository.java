package com.shopverse.domain.port;

import com.shopverse.domain.model.Review;

import java.util.List;

/**
 * Output port for product reviews.
 * Implemented in the infrastructure layer by MongoReviewRepositoryAdapter.
 */
public interface ReviewRepository {

    Review save(Review review);

    List<Review> findByProductId(Long productId);

    List<Review> findByProductIdAndMinRating(Long productId, int minRating);

    RatingStats aggregateRating(Long productId);

    /** Aggregated rating stats — returned from MongoDB aggregation pipeline. */
    record RatingStats(Double averageRating, Long reviewCount) {}
}
