package com.shopverse.web.controller;

import com.shopverse.application.usecase.review.GetReviewsUseCase;
import com.shopverse.application.usecase.review.ReviewWithCustomer;
import com.shopverse.application.usecase.review.SubmitReviewUseCase;
import com.shopverse.domain.port.ReviewRepository;
import com.shopverse.shared.ApiResponse;
import com.shopverse.web.dto.RatingStatsResponse;
import com.shopverse.web.dto.ReviewRequest;
import com.shopverse.web.dto.ReviewResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ch06-05: Reviews REST controller — backed by MongoDB, enriched with PostgreSQL customer data.
 *
 * POST   /api/products/{id}/reviews          — submit a review (authenticated)
 * GET    /api/products/{id}/reviews          — list reviews with customer info (public)
 * GET    /api/products/{id}/reviews/stats    — average rating + count (public)
 */
@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {

    private final SubmitReviewUseCase submitReviewUseCase;
    private final GetReviewsUseCase   getReviewsUseCase;

    public ReviewController(SubmitReviewUseCase submitReviewUseCase,
                            GetReviewsUseCase getReviewsUseCase) {
        this.submitReviewUseCase = submitReviewUseCase;
        this.getReviewsUseCase   = getReviewsUseCase;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> submit(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest req) {
        ReviewWithCustomer saved = submitReviewUseCase.execute(
                productId,
                req.customerId(),
                req.rating(),
                req.title(),
                req.body(),
                req.tags(),
                req.verified()
        );
        return ResponseEntity.status(201)
                .body(ApiResponse.ok(ReviewResponse.from(saved), "Review submitted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> list(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer minRating) {
        List<ReviewWithCustomer> reviews = minRating != null
                ? getReviewsUseCase.getByProductWithMinRating(productId, minRating)
                : getReviewsUseCase.getByProduct(productId);
        return ResponseEntity.ok(ApiResponse.ok(
                reviews.stream().map(ReviewResponse::from).toList()));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<RatingStatsResponse>> stats(
            @PathVariable Long productId) {
        ReviewRepository.RatingStats stats = getReviewsUseCase.getRatingStats(productId);
        return ResponseEntity.ok(ApiResponse.ok(
                new RatingStatsResponse(productId, stats.averageRating(), stats.reviewCount())));
    }
}
