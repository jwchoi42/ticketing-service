package dev.ticketing.core.reservation.application.service;

import dev.ticketing.core.reservation.application.port.in.CreateReservationCommand;
import dev.ticketing.core.reservation.application.port.in.CreateReservationUseCase;
import dev.ticketing.core.reservation.application.port.out.persistence.RecordReservationPort;
import dev.ticketing.core.reservation.domain.Reservation;
import dev.ticketing.core.reservation.domain.ReservationStatus;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationPort;
import dev.ticketing.core.site.application.port.out.persistence.allocation.RecordAllocationPort;
import dev.ticketing.core.site.domain.allocation.Allocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService implements CreateReservationUseCase {

    private final RecordReservationPort recordReservationPort;
    private final LoadAllocationPort loadAllocationPort;
    private final RecordAllocationPort recordAllocationPort;

    @Override
    @Transactional
    public Reservation createReservation(CreateReservationCommand command) {
        Long userId = command.userId();
        Long matchId = command.matchId();
        List<Long> seatIds = command.seatIds();

        // 1. Verify all seats are held by the user
        for (Long seatId : seatIds) {
            Allocation allocation = loadAllocationPort.loadAllocationWithLock(matchId, seatId)
                    .orElseThrow(() -> new IllegalArgumentException("Allocation not found for seat: " + seatId));

            if (!allocation.isHeldBy(userId)) {
                throw new IllegalStateException("Seat " + seatId + " is not held by user " + userId);
            }

            if (allocation.getHoldExpiresAt() != null && allocation.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
                throw new IllegalStateException("Seat " + seatId + " hold has expired");
            }
        }

        // 2. Create Reservation (PENDING)
        Reservation reservation = Reservation.create(userId, matchId, ReservationStatus.PENDING);
        Reservation savedReservation = recordReservationPort.record(reservation);

        // 3. Update Allocations with Reservation ID
        for (Long seatId : seatIds) {
            Allocation allocation = loadAllocationPort.loadAllocationWithLock(matchId, seatId).get();
            Allocation updatedAllocation = Allocation.withId(
                    allocation.getId(),
                    allocation.getUserId(),
                    allocation.getMatchId(),
                    allocation.getSeatId(),
                    savedReservation.getId(),
                    allocation.getStatus(),
                    allocation.getHoldExpiresAt(),
                    LocalDateTime.now());
            recordAllocationPort.recordAllocation(updatedAllocation);
        }

        // 4. Return reservation with seatIds for API response
        return Reservation.withSeatIds(
                savedReservation.getId(),
                savedReservation.getUserId(),
                savedReservation.getMatchId(),
                savedReservation.getStatus(),
                seatIds);
    }
}
