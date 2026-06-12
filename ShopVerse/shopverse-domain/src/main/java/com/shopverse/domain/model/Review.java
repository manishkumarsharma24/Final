package com.shopverse.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Domain model for a product review — pure Java, no framework dependencies.
 * Stored in MongoDB via the infrastructure layer.
 */
public class Review {

    private String id;           // MongoDB ObjectId string
    private Long productId;
    private Long customerId;
    private int rating;          // 1–5
    private String title;
    private String body;
    private List<String> tags;
    private int helpfulVotes;
    private boolean verified;
    private Instant createdAt;

    public Review() {}

    public Review(String id, Long productId, Long customerId,
                  int rating, String title, String body,
                  List<String> tags, int helpfulVotes,
                  boolean verified, Instant createdAt) {
        this.id           = id;
        this.productId    = productId;
        this.customerId   = customerId;
        this.rating       = rating;
        this.title        = title;
        this.body         = body;
        this.tags         = tags;
        this.helpfulVotes = helpfulVotes;
        this.verified     = verified;
        this.createdAt    = createdAt;
    }

    public String getId()                    { return id; }
    public void setId(String id)             { this.id = id; }
    public Long getProductId()               { return productId; }
    public void setProductId(Long v)         { this.productId = v; }
    public Long getCustomerId()              { return customerId; }
    public void setCustomerId(Long v)        { this.customerId = v; }
    public int getRating()                   { return rating; }
    public void setRating(int v)             { this.rating = v; }
    public String getTitle()                 { return title; }
    public void setTitle(String v)           { this.title = v; }
    public String getBody()                  { return body; }
    public void setBody(String v)            { this.body = v; }
    public List<String> getTags()            { return tags; }
    public void setTags(List<String> v)      { this.tags = v; }
    public int getHelpfulVotes()             { return helpfulVotes; }
    public void setHelpfulVotes(int v)       { this.helpfulVotes = v; }
    public boolean isVerified()              { return verified; }
    public void setVerified(boolean v)       { this.verified = v; }
    public Instant getCreatedAt()            { return createdAt; }
    public void setCreatedAt(Instant v)      { this.createdAt = v; }
}
