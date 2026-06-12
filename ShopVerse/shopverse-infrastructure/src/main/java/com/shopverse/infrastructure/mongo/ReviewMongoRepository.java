package com.shopverse.infrastructure.mongo;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * Ch06-05: Spring Data MongoDB repository for ReviewDocument.
 * Named ReviewMongoRepository (not ReviewRepository) to avoid naming conflict
 * with the domain port com.shopverse.domain.port.ReviewRepository.
 */
public interface ReviewMongoRepository extends MongoRepository<ReviewDocument, String> {

    List<ReviewDocument> findByProductIdOrderByCreatedAtDesc(Long productId);

    @Query("{ 'product_id': ?0, 'rating': { $gte: ?1 } }")
    List<ReviewDocument> findByProductIdAndRatingAtLeast(Long productId, int minRating);

    @Aggregation(pipeline = {
        "{ $match: { 'product_id': ?0 } }",
        "{ $group: { _id: '$product_id', avg: { $avg: '$rating' }, count: { $sum: 1 } } }"
    })
    RatingStats aggregateRating(Long productId);

    interface RatingStats {
        Double getAvg();
        Long getCount();
    }
}
