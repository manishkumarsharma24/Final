package com.shopverse.web.dto;

import com.shopverse.application.usecase.review.ReviewWithCustomer;
import com.shopverse.domain.model.Customer;
import com.shopverse.domain.model.Review;

import java.time.Instant;
import java.util.List;

/** Response DTO for a single review, including the author's customer summary. */
public record ReviewResponse(
        String id,
        Long productId,
        int rating,
        String title,
        String body,
        List<String> tags,
        int helpfulVotes,
        boolean verified,
        Instant createdAt,
        CustomerSummary customer
) {
    /** Compact customer info embedded in each review response. */
    public record CustomerSummary(Long id, String name, String email) {
        public static CustomerSummary from(Customer c) {
            if (c == null) return null;
            return new CustomerSummary(c.getId(), c.getFullName(), c.getEmail());
        }
    }

    public static ReviewResponse from(ReviewWithCustomer rwc) {
        Review r = rwc.review();
        return new ReviewResponse(
                r.getId(),
                r.getProductId(),
                r.getRating(),
                r.getTitle(),
                r.getBody(),
                r.getTags(),
                r.getHelpfulVotes(),
                r.isVerified(),
                r.getCreatedAt(),
                CustomerSummary.from(rwc.customer())
        );
    }
}
