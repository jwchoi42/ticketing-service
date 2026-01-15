package dev.ticketing.core.reservation.application.port.in;

import dev.ticketing.core.reservation.domain.Reservation;

public interface CreateReservationUseCase {
    Reservation createReservation(CreateReservationCommand command);
}
