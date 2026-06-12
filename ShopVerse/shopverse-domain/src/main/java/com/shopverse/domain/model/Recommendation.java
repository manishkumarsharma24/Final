package com.shopverse.domain.model;

/**
 * Domain model for a product recommendation from the Neo4j graph.
 */
public class Recommendation {

    private final Long productId;
    private final String name;
    private final String category;
    private final double avgRating;
    private final String reason; // e.g. "Frequently bought together", "Top rated in category"

    public Recommendation(Long productId, String name, String category,
                          double avgRating, String reason) {
        this.productId = productId;
        this.name      = name;
        this.category  = category;
        this.avgRating = avgRating;
        this.reason    = reason;
    }

    public Long getProductId()  { return productId; }
    public String getName()     { return name; }
    public String getCategory() { return category; }
    public double getAvgRating(){ return avgRating; }
    public String getReason()   { return reason; }
}
