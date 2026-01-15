package dev.ticketing.core.site.application.port.out.persistence.allocation;

import dev.ticketing.core.site.domain.allocation.Allocation;

import java.util.List;
import java.util.Optional;

public interface LoadAllocationPort {
    Optional<Allocation> loadAllocationWithLock(Long matchId, Long seatId);

    Optional<Allocation> loadAllocationByMatchAndSeat(Long matchId, Long seatId);

    List<Allocation> loadAllocationsByReservationId(Long reservationId);
}
