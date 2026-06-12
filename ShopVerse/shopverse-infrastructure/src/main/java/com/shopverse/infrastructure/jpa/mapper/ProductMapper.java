package com.shopverse.infrastructure.jpa.mapper;

import com.shopverse.domain.model.Product;
import com.shopverse.domain.vo.Money;
import com.shopverse.infrastructure.jpa.entity.ProductEntity;
import org.mapstruct.*;

/**
 * Ch04-07: MapStruct mapper — domain ↔ JPA entity conversion.
 * Avoids hand-written boilerplate; compile-time generated code.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "price", expression = "java(toMoney(entity))")
    Product toDomain(ProductEntity entity);

    @Mapping(target = "price", source = "domain.price.amount")
    @Mapping(target = "currency", source = "domain.price.currency")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductEntity toEntity(Product domain);

    /**
     * UPDATE path: copies domain fields onto an existing managed entity so
     * Hibernate's @Version field is never touched (avoids "uninitialized version" error).
     * version / createdAt / updatedAt are intentionally left on the managed entity.
     */
    @Mapping(target = "price", source = "domain.price.amount")
    @Mapping(target = "currency", source = "domain.price.currency")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(Product domain, @MappingTarget ProductEntity entity);

    default Money toMoney(ProductEntity e) {
        return new Money(e.getPrice(), e.getCurrency());
    }
}
