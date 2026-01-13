package dev.ticketing.core.site.application.service;

import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationPort;
import dev.ticketing.core.site.application.port.out.persistence.allocation.RecordAllocationPort;
import dev.ticketing.core.site.application.port.in.allocation.AllocateSeatCommand;
import dev.ticketing.core.site.application.port.in.allocation.AllocateSeatUseCase;
import dev.ticketing.core.site.application.port.in.allocation.ConfirmSeatsCommand;
import dev.ticketing.core.site.application.port.in.allocation.ConfirmSeatsUseCase;
import dev.ticketing.core.site.application.port.in.allocation.ReleaseSeatCommand;
import dev.ticketing.core.site.application.port.in.allocation.ReleaseSeatUseCase;
import dev.ticketing.core.site.application.port.out.persistence.hierarchy.LoadSeatPort;
import dev.ticketing.core.site.application.service.exception.AllocationNotFoundException;
import dev.ticketing.core.site.application.service.exception.SeatNotFoundException;
import dev.ticketing.core.site.application.service.exception.SeatAlreadyHeldException;
import dev.ticketing.core.site.application.service.exception.SeatAlreadyOccupiedException;
import dev.ticketing.core.site.application.service.exception.UnauthorizedSeatReleaseException;
import dev.ticketing.core.site.application.service.exception.NoSeatsToConfirmException;
import dev.ticketing.core.site.domain.allocation.Allocation;
import dev.ticketing.core.site.domain.allocation.AllocationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationService implements AllocateSeatUseCase, ReleaseSeatUseCase, ConfirmSeatsUseCase {

    private final RecordAllocationPort recordAllocationPort;
    private final LoadAllocationPort loadAllocationPort;
    private final LoadSeatPort loadSeatPort;

    @Value("${seat.occupation.ttl-minutes:5}")
    private int occupationTtlMinutes;

    @Override
    @Transactional
    public void allocateSeat(AllocateSeatCommand command) {
        Long userId = command.userId();
        Long matchId = command.matchId();
        Long seatId = command.seatId();

        log.info("[RDB MVP] Hold request: userId={}, matchId={}, seatId={}", userId, matchId, seatId);

        loadSeatPort.loadSeatById(seatId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));

        Allocation allocation = loadAllocationPort.loadAllocationWithLock(matchId, seatId)
                .orElse(Allocation.available(seatId));

        LocalDateTime now = LocalDateTime.now();

        // Already occupied (Final)
        if (allocation.getStatus() == AllocationStatus.OCCUPIED) {
            log.warn("Seat already occupied: userId={}, matchId={}, seatId={}", userId, matchId, seatId);
            throw new SeatAlreadyOccupiedException(matchId, seatId);
        }

        // Already held by someone else (not expired)
        if (allocation.getStatus() == AllocationStatus.HOLD &&
                allocation.getHoldExpiresAt() != null && allocation.getHoldExpiresAt().isAfter(now)) {
            if (allocation.getUserId() != null && allocation.getUserId().equals(userId)) {
                log.info("Seat already held by same user: userId={}, matchId={}, seatId={}", userId,
                        matchId, seatId);
                return;
            }
            log.warn("Seat currently held by another user: userId={}, matchId={}, seatId={}", userId,
                    matchId, seatId);
            throw new SeatAlreadyHeldException(matchId, seatId);
        }

        // Proceed to Hold
        LocalDateTime expiresAt = now.plusMinutes(occupationTtlMinutes);
        Allocation newAllocation = Allocation.withId(
                allocation.getId(),
                userId,
                matchId,
                seatId,
                null,
                AllocationStatus.HOLD,
                expiresAt,
                LocalDateTime.now());

        Allocation saved = recordAllocationPort.recordAllocation(newAllocation);
        System.out.println("[DEBUG] Seat held: userId=" + userId + ", matchId=" + matchId + ", seatId=" + seatId + ", updatedAt=" + saved.getUpdatedAt());
        log.info("Seat held successfully: userId={}, matchId={}, seatId={}, expiresAt={}, updatedAt={}", userId, matchId,
                seatId, expiresAt, saved.getUpdatedAt());
    }

    @Override
    @Transactional
    public void releaseSeat(ReleaseSeatCommand command) {
        Long userId = command.userId();
        Long matchId = command.matchId();
        Long seatId = command.seatId();

        log.info("[RDB MVP] Release request: userId={}, matchId={}, seatId={}", userId, matchId, seatId);

        Allocation allocation = loadAllocationPort.loadAllocationWithLock(matchId, seatId)
                .orElseThrow(() -> new AllocationNotFoundException(matchId, seatId));

        // Check if it's held by the same user
        if (allocation.getStatus() == AllocationStatus.HOLD &&
                allocation.getUserId() != null && allocation.getUserId().equals(userId)) {

            Allocation releasedAllocation = Allocation.withId(
                    allocation.getId(),
                    null,
                    matchId,
                    seatId,
                    null,
                    AllocationStatus.AVAILABLE,
                    null,
                    LocalDateTime.now());

            Allocation saved = recordAllocationPort.recordAllocation(releasedAllocation);
            log.info("Seat released successfully: userId={}, matchId={}, seatId={}, updatedAt={}", userId, matchId, seatId, saved.getUpdatedAt());
            return;
        }

        throw new UnauthorizedSeatReleaseException(matchId, seatId, userId);
    }

    @Override
    @Transactional
    public List<Allocation> confirmSeats(ConfirmSeatsCommand command) {
        Long userId = command.userId();
        Long matchId = command.matchId();
        List<Long> seatIds = command.seatIds();
        log.info("[RDB MVP] Confirming seats for userId={}, matchId={}, requestedSeats={}", userId, matchId, seatIds);

        List<Allocation> confirmedSeats = new ArrayList<>();
        // TODO: Implement reservation persistence

        if (confirmedSeats.isEmpty()) {
            throw new NoSeatsToConfirmException(userId, matchId, seatIds);
        }

        return confirmedSeats;
    }
}
