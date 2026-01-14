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

    public static Payment create(Long reservationId, Integer amount, String method) {
        return Payment.builder()
                .reservationId(reservationId)
                .amount(amount)
                .method(method)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Payment withId(Long id, Long reservationId, Integer amount, String method, PaymentStatus status,
                                 String paymentGatewayProvider, String paymentTransactionId, LocalDateTime createdAt, LocalDateTime paidAt) {
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
}
