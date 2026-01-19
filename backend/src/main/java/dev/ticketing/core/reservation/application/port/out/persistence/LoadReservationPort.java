package dev.ticketing.core.reservation.application.port.out.persistence;

import dev.ticketing.core.reservation.domain.Reservation;

import java.util.List;
import java.util.Optional;

public interface LoadReservationPort {
    Optional<Reservation> loadById(Long reservationId);

    List<Reservation> loadByUserId(Long userId);
}
