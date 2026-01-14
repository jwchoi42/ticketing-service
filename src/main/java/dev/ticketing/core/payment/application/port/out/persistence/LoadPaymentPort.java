package dev.ticketing.core.payment.application.port.out.persistence;

import dev.ticketing.core.payment.domain.Payment;

import java.util.Optional;

public interface LoadPaymentPort {
    Optional<Payment> loadById(Long paymentId);

    Optional<Payment> loadByReservationId(Long reservationId);
}
