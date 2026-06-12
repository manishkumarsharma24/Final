package com.shopverse.web.graphql;

import com.shopverse.application.query.SearchProductsQuery;
import com.shopverse.application.usecase.product.SearchProductsUseCase;
import com.shopverse.domain.model.Product;
import com.shopverse.infrastructure.neo4j.ProductGraphRepository;
import com.shopverse.infrastructure.neo4j.ProductNode;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * Ch12-05: Spring for GraphQL controller — @QueryMapping, @MutationMapping.
 */
@Controller
public class ProductGraphQLController {

    private final SearchProductsUseCase searchUseCase;
    private final ProductGraphRepository graphRepo;

    public ProductGraphQLController(SearchProductsUseCase searchUseCase,
                                    ProductGraphRepository graphRepo) {
        this.searchUseCase = searchUseCase;
        this.graphRepo     = graphRepo;
    }

    @QueryMapping
    public List<Product> products(@Argument String keyword,
                                  @Argument String category,
                                  @Argument Integer page,
                                  @Argument Integer size) {
        return searchUseCase.execute(new SearchProductsQuery(
                keyword, category,
                page  != null ? page  : 0,
                size  != null ? size  : 20));
    }

    @QueryMapping
    public List<ProductNode> productRecommendations(@Argument Long productId) {
        if (productId == null) return List.of();
        return graphRepo.findRecommendations(productId);
    }
}
