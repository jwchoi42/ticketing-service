package dev.ticketing.core.payment.application.service;

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

import dev.ticketing.core.payment.application.service.exception.InvalidPaymentStateException;
import dev.ticketing.core.payment.application.service.exception.PaymentNotFoundException;
import dev.ticketing.core.reservation.application.service.exception.ReservationNotFoundException;
import dev.ticketing.core.reservation.application.service.exception.InvalidReservationStateException;

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
    public Payment requestPayment(final RequestPaymentCommand command) {
        // 1. Validate Reservation
        final Reservation reservation = loadReservationPort.loadById(command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId()));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidReservationStateException(reservation.getId(), reservation.getStatus());
        }

        // 2. Create Payment (PENDING)
        final Payment payment = Payment.create(
                command.reservationId(),
                command.amount(),
                command.method());

        return recordPaymentPort.record(payment);
    }

    @Override
    @Transactional
    public Payment confirmPayment(final ConfirmPaymentCommand command) {
        // 1. Load Payment
        final Payment payment = loadPaymentPort.loadById(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(command.paymentId(), payment.getStatus());
        }

        // 2. Execute Payment via Mock Gateway
        final boolean success = paymentGatewayPort.executePayment(command.paymentKey(), command.orderId(),
                command.amount());

        if (!success) {
            // Record failure
            final Payment failedPayment = payment.markFailed("TOSS_PAYMENTS", command.paymentKey());
            return recordPaymentPort.record(failedPayment);
        }

        // 3. Update Payment to PAID
        final Payment paidPayment = payment.markPaid("TOSS_PAYMENTS", command.paymentKey());
        final Payment savedPayment = recordPaymentPort.record(paidPayment);

        // 4. Update Reservation to CONFIRMED
        final Reservation reservation = loadReservationPort.loadById(payment.getReservationId())
                .orElseThrow(() -> new ReservationNotFoundException(payment.getReservationId()));
        final Reservation confirmedReservation = reservation.confirm();
        recordReservationPort.record(confirmedReservation);

        // 5. Update Allocations to OCCUPIED
        final List<Allocation> allocations = loadAllocationPort.loadAllocationsByReservationId(reservation.getId());
        for (final Allocation allocation : allocations) {
            final Allocation occupiedAllocation = allocation.occupy();
            recordAllocationPort.recordAllocation(occupiedAllocation);
        }

        return savedPayment;
    }
}
