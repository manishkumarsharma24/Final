package com.shopverse.application.usecase.review;

import com.shopverse.domain.model.Customer;
import com.shopverse.domain.model.Review;

/**
 * Application-layer composite — pairs a Review with its author's Customer data.
 * Used as the return type for all review use cases so the web layer
 * never has to make a second lookup.
 */
public record ReviewWithCustomer(Review review, Customer customer) {}
