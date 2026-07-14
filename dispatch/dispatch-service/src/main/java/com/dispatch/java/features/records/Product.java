package com.dispatch.java.features.records;

public record Product(int id, String name, int price, String description) {

    public Product{
        if(price < 0){
            throw new IllegalArgumentException("price can't be negative");
        }
    }
}
