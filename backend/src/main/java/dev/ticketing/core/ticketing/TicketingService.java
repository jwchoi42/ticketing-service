package dev.ticketing.core.ticketing;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import dev.ticketing.core.payment.application.port.in.ConfirmPaymentCommand;
import dev.ticketing.core.payment.application.port.out.gateway.PaymentGatewayPort;
import dev.ticketing.core.payment.application.port.out.persistence.LoadPaymentPort;
import dev.ticketing.core.payment.application.port.out.persistence.RecordPaymentPort;
import dev.ticketing.core.payment.application.service.exception.InvalidPaymentStateException;
import dev.ticketing.core.payment.application.service.exception.PaymentNotFoundException;
import dev.ticketing.core.payment.domain.Payment;
import dev.ticketing.core.payment.domain.PaymentStatus;
import dev.ticketing.core.reservation.application.port.out.persistence.LoadReservationPort;
import dev.ticketing.core.reservation.application.port.out.persistence.RecordReservationPort;
import dev.ticketing.core.reservation.application.service.exception.ReservationNotFoundException;
import dev.ticketing.core.reservation.domain.Reservation;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationPort;
import dev.ticketing.core.site.application.port.out.persistence.allocation.RecordAllocationPort;
import dev.ticketing.core.site.domain.allocation.Allocation;

/**
 * 여러 도메인에 걸친 티켓팅 워크플로우를 조율하는 오케스트레이션 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketingService {

    // Payment domain ports
    private final LoadPaymentPort loadPaymentPort;
    private final RecordPaymentPort recordPaymentPort;
    private final PaymentGatewayPort paymentGatewayPort;

    // Reservation domain ports
    private final LoadReservationPort loadReservationPort;
    private final RecordReservationPort recordReservationPort;

    // Site domain ports
    private final LoadAllocationPort loadAllocationPort;
    private final RecordAllocationPort recordAllocationPort;

    /**
     * Orchestration
     * 결제 확인 및 예약 확정 워크플로우
     * Payment → Reservation → Allocation 도메인을 조율
     */
    @Transactional
    public Payment confirmPaymentAndFinalizeReservation(final ConfirmPaymentCommand command) {
        log.info("Starting payment confirmation workflow: paymentId={}", command.paymentId());

        // 1. Load and validate Payment
        final Payment payment = loadPaymentPort.loadById(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(command.paymentId(), payment.getStatus());
        }

        // 2. Execute Payment via Gateway
        final boolean success = paymentGatewayPort.executePayment(
                command.paymentKey(),
                command.orderId(),
                command.amount());

        if (!success) {
            log.warn("Payment execution failed: paymentId={}", command.paymentId());
            final Payment failedPayment = payment.markFailed("TOSS_PAYMENTS", command.paymentKey());
            return recordPaymentPort.record(failedPayment);
        }

        // 3. Update Payment to PAID
        final Payment paidPayment = payment.markPaid("TOSS_PAYMENTS", command.paymentKey());
        final Payment savedPayment = recordPaymentPort.record(paidPayment);
        log.info("Payment marked as paid: paymentId={}", savedPayment.getId());

        // 4. Update Reservation to CONFIRMED
        final Reservation reservation = loadReservationPort.loadById(payment.getReservationId())
                .orElseThrow(() -> new ReservationNotFoundException(payment.getReservationId()));
        final Reservation confirmedReservation = reservation.confirm();
        recordReservationPort.record(confirmedReservation);
        log.info("Reservation confirmed: reservationId={}", reservation.getId());

        // 5. Update Allocations to OCCUPIED
        final List<Allocation> allocations = loadAllocationPort.loadAllocationsByReservationId(reservation.getId());
        for (final Allocation allocation : allocations) {
            final Allocation occupiedAllocation = allocation.occupy();
            recordAllocationPort.recordAllocation(occupiedAllocation);
        }
        log.info("Allocations marked as occupied: count={}", allocations.size());

        return savedPayment;
    }
}
