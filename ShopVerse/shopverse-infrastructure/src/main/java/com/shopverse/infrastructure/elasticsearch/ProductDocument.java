package com.shopverse.infrastructure.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.suggest.Completion;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ch08-01: Elasticsearch document — product search index.
 * Ch08-03: Multi-field mapping: text (analyzed) + keyword (exact).
 * Ch08-04: Completion suggester for autocomplete.
 */
@Document(indexName = "products")
@Setting(settingPath = "elasticsearch/product-settings.json")
public class ProductDocument {

    @Id
    private String id;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "standard"),
                otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(name = "stock_quantity", type = FieldType.Integer)
    private int stockQuantity;

    @Field(type = FieldType.Boolean)
    private boolean active;

    @Field(name = "avg_rating", type = FieldType.Double)
    private Double avgRating;

    @Field(name = "review_count", type = FieldType.Integer)
    private Integer reviewCount;

    @CompletionField(maxInputLength = 100)
    private Completion suggest;   // Ch08-04: autocomplete

    @Field(name = "created_at", type = FieldType.Date)
    private Instant createdAt;

    // Getters & setters
    public String getId()                           { return id; }
    public void setId(String id)                    { this.id = id; }
    public String getName()                         { return name; }
    public void setName(String name)                { this.name = name; }
    public String getDescription()                  { return description; }
    public void setDescription(String desc)         { this.description = desc; }
    public String getCategory()                     { return category; }
    public void setCategory(String cat)             { this.category = cat; }
    public BigDecimal getPrice()                    { return price; }
    public void setPrice(BigDecimal price)          { this.price = price; }
    public int getStockQuantity()                   { return stockQuantity; }
    public void setStockQuantity(int qty)           { this.stockQuantity = qty; }
    public boolean isActive()                       { return active; }
    public void setActive(boolean active)           { this.active = active; }
    public Double getAvgRating()                    { return avgRating; }
    public void setAvgRating(Double r)              { this.avgRating = r; }
    public Completion getSuggest()                      { return suggest; }
    public void setSuggest(Completion suggest)          { this.suggest = suggest; }
    public Instant getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(Instant t)             { this.createdAt = t; }
}
