package dev.ticketing.core.payment.adapter.in.web.model.response;

import dev.ticketing.core.payment.domain.Payment;
import dev.ticketing.core.payment.domain.PaymentStatus;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long reservationId,
        Integer amount,
        String method,
        PaymentStatus status,
        String paymentGatewayProvider,
        String paymentTransactionId,
        LocalDateTime createdAt,
        LocalDateTime paidAt) {
    public static PaymentResponse from(final Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReservationId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getPaymentGatewayProvider(),
                payment.getPaymentTransactionId(),
                payment.getCreatedAt(),
                payment.getPaidAt());
    }
}
