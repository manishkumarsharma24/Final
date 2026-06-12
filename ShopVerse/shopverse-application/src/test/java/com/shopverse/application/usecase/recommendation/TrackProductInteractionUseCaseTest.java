package com.shopverse.application.usecase.recommendation;

import com.shopverse.domain.event.AnalyticsEvent;
import com.shopverse.domain.port.EventPublisher;
import com.shopverse.domain.port.RecommendationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackProductInteractionUseCaseTest {

    @Mock private RecommendationRepository recommendationRepository;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks
    private TrackProductInteractionUseCase useCase;

    @Test
    void trackOrderPurchase_upsertsNodesAndCreatesPairs() {
        List<Long>   productIds   = List.of(1L, 2L, 3L);
        List<String> names        = List.of("A", "B", "C");
        List<String> categories   = List.of("X", "Y", "Z");

        useCase.trackOrderPurchase(10L, productIds, names, categories, 100L, "sess-1");

        // 3 product upserts
        verify(recommendationRepository, times(3)).upsertProductNode(anyLong(), anyString(), anyString(), anyDouble());

        // 3 pairs: (1,2), (1,3), (2,3)
        verify(recommendationRepository).recordPurchasedTogether(1L, 2L, 10L);
        verify(recommendationRepository).recordPurchasedTogether(1L, 3L, 10L);
        verify(recommendationRepository).recordPurchasedTogether(2L, 3L, 10L);

        // Analytics event published
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(AnalyticsEvent.OrderConverted.class);
    }

    @Test
    void trackProductView_upsertNodeAndRecordsViewedAfter() {
        useCase.trackProductView(5L, "Phone", "Electronics", 200L, "sess-2", 4L);

        verify(recommendationRepository).upsertProductNode(5L, "Phone", "Electronics", 0.0);
        verify(recommendationRepository).recordViewedAfter(4L, 5L, "sess-2");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        AnalyticsEvent.ProductViewed event = (AnalyticsEvent.ProductViewed) captor.getValue();
        assertThat(event.productId()).isEqualTo(5L);
        assertThat(event.sessionId()).isEqualTo("sess-2");
    }

    @Test
    void trackProductView_noPreviousProduct_noViewedAfterRelationship() {
        useCase.trackProductView(5L, "Phone", "Electronics", 200L, "sess-3", null);

        verify(recommendationRepository, never()).recordViewedAfter(any(), any(), any());
        verify(eventPublisher).publish(any(AnalyticsEvent.ProductViewed.class));
    }
}
