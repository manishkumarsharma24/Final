package com.shopverse.infrastructure.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

/**
 * Ch06-06: Neo4j node entity — Product in recommendations graph.
 * Edges: FREQUENTLY_BOUGHT_TOGETHER, IN_CATEGORY, VIEWED_AFTER.
 */
@Node("Product")
public class ProductNode {

    @Id @GeneratedValue
    private Long id;

    private Long productId;
    private String name;
    private String category;
    private double avgRating;

    @Relationship(type = "FREQUENTLY_BOUGHT_TOGETHER", direction = Relationship.Direction.OUTGOING)
    private List<ProductNode> frequentlyBoughtWith;

    @Relationship(type = "VIEWED_AFTER", direction = Relationship.Direction.OUTGOING)
    private List<ProductNode> viewedAfter;

    public ProductNode() {}

    public ProductNode(Long productId, String name, String category) {
        this.productId = productId;
        this.name      = name;
        this.category  = category;
    }

    public Long getId()                                         { return id; }
    public Long getProductId()                                  { return productId; }
    public String getName()                                     { return name; }
    public String getCategory()                                 { return category; }
    public double getAvgRating()                                { return avgRating; }
    public void setAvgRating(double r)                          { this.avgRating = r; }
    public List<ProductNode> getFrequentlyBoughtWith()          { return frequentlyBoughtWith; }
    public List<ProductNode> getViewedAfter()                   { return viewedAfter; }
}
