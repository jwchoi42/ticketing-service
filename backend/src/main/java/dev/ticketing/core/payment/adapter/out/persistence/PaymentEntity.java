package dev.ticketing.core.payment.adapter.out.persistence;

import dev.ticketing.core.payment.domain.Payment;
import dev.ticketing.core.payment.domain.PaymentStatus;
import dev.ticketing.core.reservation.adapter.out.persistence.ReservationEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private ReservationEntity reservation;
    private Integer amount;
    private String method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private String paymentGatewayProvider;
    private String paymentTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    public static PaymentEntity from(final Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.id = payment.getId();
        entity.reservation = ReservationEntity.fromId(payment.getReservationId());
        entity.amount = payment.getAmount();
        entity.method = payment.getMethod();
        entity.status = payment.getStatus();
        entity.paymentGatewayProvider = payment.getPaymentGatewayProvider();
        entity.paymentTransactionId = payment.getPaymentTransactionId();
        entity.createdAt = payment.getCreatedAt();
        entity.paidAt = payment.getPaidAt();
        return entity;
    }

    public Payment toDomain() {
        return Payment.withId(
                id,
                reservation != null ? reservation.getId() : null,
                amount,
                method,
                status,
                paymentGatewayProvider,
                paymentTransactionId,
                createdAt,
                paidAt);
    }
}
