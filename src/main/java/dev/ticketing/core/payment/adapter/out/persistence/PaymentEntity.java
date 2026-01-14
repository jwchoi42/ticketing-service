package dev.ticketing.core.payment.adapter.out.persistence;

import dev.ticketing.core.payment.domain.Payment;
import dev.ticketing.core.payment.domain.PaymentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "payments")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long reservationId;
    private Integer amount;
    private String method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private String paymentGatewayProvider;
    private String paymentTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    public static PaymentEntity from(Payment payment) {
        return new PaymentEntity(
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

    public Payment toDomain() {
        return Payment.withId(id, reservationId, amount, method, status, paymentGatewayProvider, paymentTransactionId,
                createdAt, paidAt);
    }
}
