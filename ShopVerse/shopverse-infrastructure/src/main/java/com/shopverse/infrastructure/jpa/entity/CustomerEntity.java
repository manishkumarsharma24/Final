package com.shopverse.infrastructure.jpa.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Ch04-01: Customer JPA entity.
 * Ch04-02: @Embedded for address.
 */
@Entity
@Table(
    name = "customers",
    indexes = { @Index(name = "idx_customers_email", columnList = "email", unique = true) }
)
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customers_seq")
    @SequenceGenerator(name = "customers_seq", sequenceName = "customers_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Embedded
    private AddressEmbeddable shippingAddress;

    @Column(name = "tier", nullable = false, length = 20)
    private String tier = "STANDARD";

    @Column(name = "loyalty_points", nullable = false)
    private int loyaltyPoints = 0;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // Getters & setters
    public Long getId()                                     { return id; }
    public void setId(Long id)                              { this.id = id; }
    public String getFirstName()                            { return firstName; }
    public void setFirstName(String fn)                     { this.firstName = fn; }
    public String getLastName()                             { return lastName; }
    public void setLastName(String ln)                      { this.lastName = ln; }
    public String getEmail()                                { return email; }
    public void setEmail(String email)                      { this.email = email; }
    public String getPhone()                                { return phone; }
    public void setPhone(String phone)                      { this.phone = phone; }
    public AddressEmbeddable getShippingAddress()           { return shippingAddress; }
    public void setShippingAddress(AddressEmbeddable addr)  { this.shippingAddress = addr; }
    public String getTier()                                 { return tier; }
    public void setTier(String tier)                        { this.tier = tier; }
    public int getLoyaltyPoints()                           { return loyaltyPoints; }
    public void setLoyaltyPoints(int pts)                   { this.loyaltyPoints = pts; }
    public LocalDate getDateOfBirth()                       { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dob)               { this.dateOfBirth = dob; }
    public boolean isActive()                               { return active; }
    public void setActive(boolean active)                   { this.active = active; }
    public Instant getCreatedAt()                           { return createdAt; }
}
