package com.shopverse.application.usecase.notification;

import com.shopverse.domain.event.NotificationEvent;
import com.shopverse.domain.port.NotificationPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SendNotificationUseCaseTest {

    @Mock private NotificationPublisher notificationPublisher;

    @InjectMocks
    private SendNotificationUseCase useCase;

    @Test
    void sendOrderConfirmation_publishesCorrectEvent() {
        useCase.sendOrderConfirmation(1L, 10L, "user@test.com", new BigDecimal("99.99"));

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationPublisher).publish(captor.capture());

        assertThat(captor.getValue()).isInstanceOf(NotificationEvent.OrderConfirmationNotification.class);
        var event = (NotificationEvent.OrderConfirmationNotification) captor.getValue();
        assertThat(event.orderId()).isEqualTo(1L);
        assertThat(event.customerEmail()).isEqualTo("user@test.com");
        assertThat(event.total()).isEqualByComparingTo("99.99");
    }

    @Test
    void sendShippingUpdate_publishesShippedEvent() {
        useCase.sendShippingUpdate(2L, "user@test.com", "TRK-999");

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationPublisher).publish(captor.capture());

        var event = (NotificationEvent.OrderShippedNotification) captor.getValue();
        assertThat(event.trackingNumber()).isEqualTo("TRK-999");
    }

    @Test
    void sendDeliveryConfirmation_publishesDeliveredEvent() {
        useCase.sendDeliveryConfirmation(3L, "user@test.com");

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationPublisher).publish(captor.capture());

        assertThat(captor.getValue()).isInstanceOf(NotificationEvent.OrderDeliveredNotification.class);
    }

    @Test
    void sendPaymentSuccess_publishesCorrectEvent() {
        useCase.sendPaymentSuccess(4L, "user@test.com", new BigDecimal("149.00"), "STRIPE");

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationPublisher).publish(captor.capture());

        var event = (NotificationEvent.PaymentSuccessNotification) captor.getValue();
        assertThat(event.paymentMethod()).isEqualTo("STRIPE");
        assertThat(event.amount()).isEqualByComparingTo("149.00");
    }

    @Test
    void sendPaymentFailed_publishesFailedEvent() {
        useCase.sendPaymentFailed(5L, "user@test.com", "Insufficient funds");

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationPublisher).publish(captor.capture());

        var event = (NotificationEvent.PaymentFailedNotification) captor.getValue();
        assertThat(event.failureReason()).isEqualTo("Insufficient funds");
    }
}
