package com.shopverse.application.usecase.recommendation;

import com.shopverse.domain.model.Recommendation;
import com.shopverse.domain.port.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetRecommendationsUseCaseTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @InjectMocks
    private GetRecommendationsUseCase useCase;

    private List<Recommendation> mockRecs;

    @BeforeEach
    void setUp() {
        mockRecs = List.of(
            new Recommendation(2L, "Laptop Stand", "Electronics", 4.5, "Frequently bought together"),
            new Recommendation(3L, "USB Hub",      "Electronics", 4.2, "Frequently bought together")
        );
    }

    @Test
    void getRecommendations_returnsListFromRepository() {
        when(recommendationRepository.findRecommendations(1L)).thenReturn(mockRecs);

        List<Recommendation> result = useCase.getRecommendations(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductId()).isEqualTo(2L);
        assertThat(result.get(0).getName()).isEqualTo("Laptop Stand");
        verify(recommendationRepository).findRecommendations(1L);
    }

    @Test
    void getRecommendations_returnsEmptyListWhenNoGraph() {
        when(recommendationRepository.findRecommendations(99L)).thenReturn(List.of());

        List<Recommendation> result = useCase.getRecommendations(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void getByCategory_returnsTopRatedInCategory() {
        List<Recommendation> catRecs = List.of(
            new Recommendation(4L, "Keyboard", "Electronics", 4.8, "Top rated in Electronics")
        );
        when(recommendationRepository.findTopRatedInCategory("Electronics", 1L)).thenReturn(catRecs);

        List<Recommendation> result = useCase.getByCategory("Electronics", 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReason()).isEqualTo("Top rated in Electronics");
    }
}
