package com.shopverse.domain.model;

import com.shopverse.domain.vo.Address;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Ch02-02: Customer domain entity.
 */
public class Customer {

    private final Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Address shippingAddress;
    private CustomerTier tier;
    private int loyaltyPoints;
    private LocalDate dateOfBirth;
    private boolean active;

    public Customer(Long id, String firstName, String lastName, String email) {
        this.id        = id;
        this.firstName = Objects.requireNonNull(firstName);
        this.lastName  = Objects.requireNonNull(lastName);
        this.email     = Objects.requireNonNull(email);
        this.tier      = CustomerTier.STANDARD;
        this.loyaltyPoints = 0;
        this.active    = true;
    }

    public void addLoyaltyPoints(int points) {
        this.loyaltyPoints += points;
        this.tier = CustomerTier.forPoints(this.loyaltyPoints);
    }

    public String getFullName() { return firstName + " " + lastName; }

    // Getters & setters
    public Long getId()                     { return id; }
    public String getFirstName()            { return firstName; }
    public String getLastName()             { return lastName; }
    public String getEmail()                { return email; }
    public String getPhone()                { return phone; }
    public Address getShippingAddress()     { return shippingAddress; }
    public CustomerTier getTier()           { return tier; }
    public int getLoyaltyPoints()           { return loyaltyPoints; }
    public LocalDate getDateOfBirth()       { return dateOfBirth; }
    public boolean isActive()               { return active; }

    public void setPhone(String phone)                      { this.phone = phone; }
    public void setShippingAddress(Address address)         { this.shippingAddress = address; }
    public void setDateOfBirth(LocalDate dob)               { this.dateOfBirth = dob; }
    public void setEmail(String email)                      { this.email = Objects.requireNonNull(email); }
    public void deactivate()                                { this.active = false; }
}
