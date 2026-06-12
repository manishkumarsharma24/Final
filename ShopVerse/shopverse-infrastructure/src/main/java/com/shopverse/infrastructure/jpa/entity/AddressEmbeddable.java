package com.shopverse.infrastructure.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Ch04-02: @Embeddable — maps Address value object columns inline. */
@Embeddable
public class AddressEmbeddable {

    @Column(name = "street", length = 255)
    private String street;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 2)
    private String country;

    public String getStreet()       { return street; }
    public void setStreet(String s) { this.street = s; }
    public String getCity()         { return city; }
    public void setCity(String c)   { this.city = c; }
    public String getState()        { return state; }
    public void setState(String s)  { this.state = s; }
    public String getPostalCode()   { return postalCode; }
    public void setPostalCode(String p) { this.postalCode = p; }
    public String getCountry()      { return country; }
    public void setCountry(String c) { this.country = c; }
}
