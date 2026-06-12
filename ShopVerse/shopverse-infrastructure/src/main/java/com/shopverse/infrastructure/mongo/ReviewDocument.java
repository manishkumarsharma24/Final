package com.shopverse.infrastructure.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * Ch06-05: MongoDB document — product reviews (flexible schema, embedded sub-docs).
 * Stored in the "reviews" collection.
 */
@Document(collection = "reviews")
public class ReviewDocument {

    @Id
    private String id;

    @Indexed
    @Field("product_id")
    private Long productId;

    @Indexed
    @Field("customer_id")
    private Long customerId;

    private int rating;         // 1-5
    private String title;
    private String body;
    private List<String> tags;

    @Field("helpful_votes")
    private int helpfulVotes;

    private boolean verified;

    @Field("created_at")
    private Instant createdAt = Instant.now();

    // Getters & setters
    public String getId()                       { return id; }
    public void setId(String id)               { this.id = id; }
    public Long getProductId()                  { return productId; }
    public void setProductId(Long pid)          { this.productId = pid; }
    public Long getCustomerId()                 { return customerId; }
    public void setCustomerId(Long cid)         { this.customerId = cid; }
    public int getRating()                      { return rating; }
    public void setRating(int rating)           { this.rating = rating; }
    public String getTitle()                    { return title; }
    public void setTitle(String title)          { this.title = title; }
    public String getBody()                     { return body; }
    public void setBody(String body)            { this.body = body; }
    public List<String> getTags()               { return tags; }
    public void setTags(List<String> tags)      { this.tags = tags; }
    public int getHelpfulVotes()                { return helpfulVotes; }
    public void setHelpfulVotes(int v)          { this.helpfulVotes = v; }
    public boolean isVerified()                 { return verified; }
    public void setVerified(boolean v)          { this.verified = v; }
    public Instant getCreatedAt()               { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
