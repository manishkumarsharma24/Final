package com.shopverse.infrastructure.adapter;

import com.shopverse.domain.model.Review;
import com.shopverse.domain.port.ReviewRepository;
import com.shopverse.infrastructure.mongo.ReviewDocument;
import com.shopverse.infrastructure.mongo.ReviewMongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Hexagonal adapter — ReviewRepository domain port → MongoDB.
 * Injects ReviewMongoRepository (Spring Data) to avoid naming conflict
 * with the domain port ReviewRepository.
 */
@Repository
public class MongoReviewRepositoryAdapter implements ReviewRepository {

    private final ReviewMongoRepository mongoRepo;

    public MongoReviewRepositoryAdapter(ReviewMongoRepository mongoRepo) {
        this.mongoRepo = mongoRepo;
    }

    @Override
    public Review save(Review review) {
        return toDomain(mongoRepo.save(toDocument(review)));
    }

    @Override
    public List<Review> findByProductId(Long productId) {
        return mongoRepo.findByProductIdOrderByCreatedAtDesc(productId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Review> findByProductIdAndMinRating(Long productId, int minRating) {
        return mongoRepo.findByProductIdAndRatingAtLeast(productId, minRating)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public ReviewRepository.RatingStats aggregateRating(Long productId) {
        ReviewMongoRepository.RatingStats mongoStats = mongoRepo.aggregateRating(productId);
        if (mongoStats == null) {
            return new ReviewRepository.RatingStats(0.0, 0L);
        }
        return new ReviewRepository.RatingStats(mongoStats.getAvg(), mongoStats.getCount());
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private ReviewDocument toDocument(Review r) {
        ReviewDocument doc = new ReviewDocument();
        doc.setId(r.getId());
        doc.setProductId(r.getProductId());
        doc.setCustomerId(r.getCustomerId());
        doc.setRating(r.getRating());
        doc.setTitle(r.getTitle());
        doc.setBody(r.getBody());
        doc.setTags(r.getTags());
        doc.setHelpfulVotes(r.getHelpfulVotes());
        doc.setVerified(r.isVerified());
        return doc;
    }

    private Review toDomain(ReviewDocument doc) {
        return new Review(
                doc.getId(),
                doc.getProductId(),
                doc.getCustomerId(),
                doc.getRating(),
                doc.getTitle(),
                doc.getBody(),
                doc.getTags(),
                doc.getHelpfulVotes(),
                doc.isVerified(),
                doc.getCreatedAt()
        );
    }
}
