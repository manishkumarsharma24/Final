package com.shopverse.infrastructure.adapter;

import com.shopverse.domain.model.Customer;
import com.shopverse.domain.model.CustomerTier;
import com.shopverse.domain.port.CustomerRepository;
import com.shopverse.domain.vo.Address;
import com.shopverse.infrastructure.jpa.entity.AddressEmbeddable;
import com.shopverse.infrastructure.jpa.entity.CustomerEntity;
import com.shopverse.infrastructure.jpa.repository.JpaCustomerRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Ch03-04: Customer domain port → JPA adapter. */
@Repository
public class JpaCustomerRepositoryAdapter implements CustomerRepository {

    private final JpaCustomerRepository jpaRepo;

    public JpaCustomerRepositoryAdapter(JpaCustomerRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Customer save(Customer c) {
        return toDomain(jpaRepo.save(toEntity(c)));
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return jpaRepo.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepo.existsByEmail(email);
    }

    @Override
    public void deleteById(Long id) { jpaRepo.deleteById(id); }

    private CustomerEntity toEntity(Customer c) {
        CustomerEntity e = new CustomerEntity();
        e.setId(c.getId());
        e.setFirstName(c.getFirstName());
        e.setLastName(c.getLastName());
        e.setEmail(c.getEmail());
        e.setPhone(c.getPhone());
        e.setTier(c.getTier().name());
        e.setLoyaltyPoints(c.getLoyaltyPoints());
        e.setDateOfBirth(c.getDateOfBirth());
        e.setActive(c.isActive());
        if (c.getShippingAddress() != null) {
            AddressEmbeddable a = new AddressEmbeddable();
            a.setStreet(c.getShippingAddress().street());
            a.setCity(c.getShippingAddress().city());
            a.setState(c.getShippingAddress().state());
            a.setPostalCode(c.getShippingAddress().postalCode());
            a.setCountry(c.getShippingAddress().country());
            e.setShippingAddress(a);
        }
        return e;
    }

    private Customer toDomain(CustomerEntity e) {
        Customer c = new Customer(e.getId(), e.getFirstName(), e.getLastName(), e.getEmail());
        c.setPhone(e.getPhone());
        c.setDateOfBirth(e.getDateOfBirth());
        if (e.getShippingAddress() != null) {
            AddressEmbeddable a = e.getShippingAddress();
            c.setShippingAddress(new Address(a.getStreet(), a.getCity(),
                    a.getState(), a.getPostalCode(), a.getCountry()));
        }
        // Restore loyalty points + tier
        int pts = e.getLoyaltyPoints();
        if (pts > 0) c.addLoyaltyPoints(pts);
        if (!e.isActive()) c.deactivate();
        return c;
    }
}
