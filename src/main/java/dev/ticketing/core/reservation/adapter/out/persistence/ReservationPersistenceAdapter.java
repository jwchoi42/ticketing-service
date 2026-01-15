package dev.ticketing.core.reservation.adapter.out.persistence;

import dev.ticketing.core.reservation.application.port.out.persistence.LoadReservationPort;
import dev.ticketing.core.reservation.application.port.out.persistence.RecordReservationPort;
import dev.ticketing.core.reservation.domain.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReservationPersistenceAdapter implements LoadReservationPort, RecordReservationPort {

    private final ReservationRepository reservationRepository;

    @Override
    public Optional<Reservation> loadById(final Long reservationId) {
        return reservationRepository.findById(reservationId).map(ReservationEntity::toDomain);
    }

    @Override
    public List<Reservation> loadByUserId(final Long userId) {
        return reservationRepository.findByUserId(userId).stream().map(ReservationEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Reservation record(final Reservation reservation) {
        return reservationRepository.save(ReservationEntity.from(reservation)).toDomain();
    }
}
