package dev.ticketing.core.payment.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Payment {
    private Long id;
    private Long reservationId;
    private Integer amount;
    private String method;
    private PaymentStatus status;
    private String paymentGatewayProvider;
    private String paymentTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    public static Payment create(final Long reservationId, final Integer amount, final String method) {
        validate(reservationId, amount, method);
        return Payment.builder()
                .reservationId(reservationId)
                .amount(amount)
                .method(method)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Payment withId(final Long id, final Long reservationId, final Integer amount, final String method,
            final PaymentStatus status,
            final String paymentGatewayProvider, final String paymentTransactionId, final LocalDateTime createdAt,
            final LocalDateTime paidAt) {
        validate(reservationId, amount, method);
        return Payment.builder()
                .id(id)
                .reservationId(reservationId)
                .amount(amount)
                .method(method)
                .status(status)
                .paymentGatewayProvider(paymentGatewayProvider)
                .paymentTransactionId(paymentTransactionId)
                .createdAt(createdAt)
                .paidAt(paidAt)
                .build();
    }

    private static void validate(final Long reservationId, final Integer amount, final String method) {
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID cannot be null");
        }
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("Payment amount must be non-negative");
        }
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("Payment method cannot be empty");
        }
    }

    public Payment markPaid(final String provider, final String transactionId) {
        return Payment.builder()
                .id(this.id)
                .reservationId(this.reservationId)
                .amount(this.amount)
                .method(this.method)
                .status(PaymentStatus.PAID)
                .paymentGatewayProvider(provider)
                .paymentTransactionId(transactionId)
                .createdAt(this.createdAt)
                .paidAt(LocalDateTime.now())
                .build();
    }

    public Payment markFailed(final String provider, final String transactionId) {
        return Payment.builder()
                .id(this.id)
                .reservationId(this.reservationId)
                .amount(this.amount)
                .method(this.method)
                .status(PaymentStatus.FAILED)
                .paymentGatewayProvider(provider)
                .paymentTransactionId(transactionId)
                .createdAt(this.createdAt)
                .paidAt(null)
                .build();
    }
}
