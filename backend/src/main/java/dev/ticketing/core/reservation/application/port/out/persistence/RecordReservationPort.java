package dev.ticketing.core.reservation.application.port.out.persistence;

import dev.ticketing.core.reservation.domain.Reservation;

public interface RecordReservationPort {
    Reservation record(Reservation reservation);
}
