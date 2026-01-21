package dev.ticketing.core.reservation.application.service;

import dev.ticketing.core.match.application.port.out.persistence.LoadMatchPort;
import dev.ticketing.core.match.application.service.exception.MatchNotFoundException;
import dev.ticketing.core.match.application.service.exception.MatchNotOpenException;
import dev.ticketing.core.match.domain.Match;
import dev.ticketing.core.reservation.application.port.in.CreateReservationCommand;
import dev.ticketing.core.reservation.application.port.in.CreateReservationUseCase;
import dev.ticketing.core.reservation.application.port.out.persistence.RecordReservationPort;
import dev.ticketing.core.reservation.domain.Reservation;
import dev.ticketing.core.reservation.domain.ReservationStatus;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationPort;
import dev.ticketing.core.site.application.port.out.persistence.allocation.RecordAllocationPort;
import dev.ticketing.core.site.domain.allocation.Allocation;
import dev.ticketing.core.reservation.application.service.exception.ReservationHoldExpiredException;
import dev.ticketing.core.reservation.application.service.exception.ReservationSeatNotHeldException;
import dev.ticketing.core.site.application.service.exception.AllocationNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService implements CreateReservationUseCase {

    private final RecordReservationPort recordReservationPort;
    private final LoadAllocationPort loadAllocationPort;
    private final RecordAllocationPort recordAllocationPort;
    private final LoadMatchPort loadMatchPort;

    @Override
    @Transactional
    public Reservation createReservation(final CreateReservationCommand command) {
        final Long userId = command.userId();
        final Long matchId = command.matchId();
        final List<Long> seatIds = command.seatIds();

        // 0. Verify match is open
        final Match match = loadMatchPort.loadById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));

        if (!match.isOpen()) {
            throw new MatchNotOpenException(matchId);
        }

        // 1. Verify all seats are held by the user
        for (final Long seatId : seatIds) {
            final Allocation allocation = loadAllocationPort.loadAllocationByMatchAndSeatWithLock(matchId, seatId)
                    .orElseThrow(() -> new AllocationNotFoundException(matchId, seatId));

            if (!allocation.isHeldBy(userId)) {
                throw new ReservationSeatNotHeldException(seatId, userId);
            }

            if (allocation.getHoldExpiresAt() != null && allocation.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
                throw new ReservationHoldExpiredException(seatId);
            }
        }

        // 2. Create Reservation (PENDING)
        final Reservation reservation = Reservation.create(userId, matchId, ReservationStatus.PENDING);
        final Reservation savedReservation = recordReservationPort.record(reservation);

        // 3. Update Allocations with Reservation ID
        for (final Long seatId : seatIds) {
            final Allocation allocation = loadAllocationPort.loadAllocationByMatchAndSeatWithLock(matchId, seatId)
                    .orElseThrow(() -> new AllocationNotFoundException(matchId, seatId));
            final Allocation updatedAllocation = allocation.assignReservation(savedReservation.getId());
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
