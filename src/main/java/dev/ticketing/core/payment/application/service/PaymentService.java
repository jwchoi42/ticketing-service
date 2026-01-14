package dev.ticketing.core.payment.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import dev.ticketing.core.payment.application.port.in.ConfirmPaymentCommand;
import dev.ticketing.core.payment.application.port.in.ConfirmPaymentUseCase;
import dev.ticketing.core.payment.application.port.in.RequestPaymentCommand;
import dev.ticketing.core.payment.application.port.in.RequestPaymentUseCase;
import dev.ticketing.core.payment.application.port.out.gateway.PaymentGatewayPort;
import dev.ticketing.core.payment.application.port.out.persistence.LoadPaymentPort;
import dev.ticketing.core.payment.application.port.out.persistence.RecordPaymentPort;
import dev.ticketing.core.payment.domain.Payment;
import dev.ticketing.core.payment.domain.PaymentStatus;
import dev.ticketing.core.reservation.application.port.out.persistence.LoadReservationPort;
import dev.ticketing.core.reservation.application.port.out.persistence.RecordReservationPort;
import dev.ticketing.core.reservation.domain.Reservation;
import dev.ticketing.core.reservation.domain.ReservationStatus;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationPort;
import dev.ticketing.core.site.application.port.out.persistence.allocation.RecordAllocationPort;
import dev.ticketing.core.site.domain.allocation.Allocation;
import dev.ticketing.core.site.domain.allocation.AllocationStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService implements RequestPaymentUseCase, ConfirmPaymentUseCase {

    private final LoadPaymentPort loadPaymentPort;
    private final RecordPaymentPort recordPaymentPort;
    private final PaymentGatewayPort paymentGatewayPort;

    // Cross-domain ports
    private final LoadReservationPort loadReservationPort;
    private final RecordReservationPort recordReservationPort;
    private final LoadAllocationPort loadAllocationPort;
    private final RecordAllocationPort recordAllocationPort;

    @Override
    @Transactional
    public Payment requestPayment(RequestPaymentCommand command) {
        // 1. Validate Reservation
        Reservation reservation = loadReservationPort.loadById(command.reservationId())
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + command.reservationId()));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Reservation is not in PENDING state");
        }

        // 2. Create Payment (PENDING)
        Payment payment = Payment.create(
                command.reservationId(),
                command.amount(),
                command.method());

        return recordPaymentPort.record(payment);
    }

    @Override
    @Transactional
    public Payment confirmPayment(ConfirmPaymentCommand command) {
        // 1. Load Payment
        Payment payment = loadPaymentPort.loadById(command.paymentId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + command.paymentId()));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment is not in PENDING state");
        }

        // 2. Execute Payment via Mock Gateway
        boolean success = paymentGatewayPort.executePayment(command.paymentKey(), command.orderId(), command.amount());

        if (!success) {
            // Record failure
            Payment failedPayment = Payment.withId(
                    payment.getId(),
                    payment.getReservationId(),
                    payment.getAmount(),
                    payment.getMethod(),
                    PaymentStatus.FAILED,
                    "TOSS_PAYMENTS",
                    command.paymentKey(),
                    payment.getCreatedAt(),
                    null);
            return recordPaymentPort.record(failedPayment);
        }

        // 3. Update Payment to PAID
        Payment paidPayment = Payment.withId(
                payment.getId(),
                payment.getReservationId(),
                payment.getAmount(),
                payment.getMethod(),
                PaymentStatus.PAID,
                "TOSS_PAYMENTS",
                command.paymentKey(),
                payment.getCreatedAt(),
                LocalDateTime.now());
        Payment savedPayment = recordPaymentPort.record(paidPayment);

        // 4. Update Reservation to CONFIRMED
        Reservation reservation = loadReservationPort.loadById(payment.getReservationId()).get();
        Reservation confirmedReservation = Reservation.withId(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getMatchId(),
                ReservationStatus.CONFIRMED);
        recordReservationPort.record(confirmedReservation);

        // 5. Update Allocations to OCCUPIED
        List<Allocation> allocations = loadAllocationPort.loadAllocationsByReservationId(reservation.getId());
        for (Allocation allocation : allocations) {
            Allocation occupiedAllocation = Allocation.withId(
                    allocation.getId(),
                    allocation.getUserId(),
                    allocation.getMatchId(),
                    allocation.getSeatId(),
                    allocation.getReservationId(),
                    AllocationStatus.OCCUPIED,
                    null, // No expiry for confirmed seats
                    LocalDateTime.now());
            recordAllocationPort.recordAllocation(occupiedAllocation);
        }

        return savedPayment;
    }
}
