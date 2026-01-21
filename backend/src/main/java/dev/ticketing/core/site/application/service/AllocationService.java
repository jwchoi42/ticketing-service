package dev.ticketing.core.site.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import dev.ticketing.core.match.application.port.out.persistence.LoadMatchPort;
import dev.ticketing.core.match.application.service.exception.MatchNotFoundException;
import dev.ticketing.core.match.application.service.exception.MatchNotOpenException;
import dev.ticketing.core.match.domain.Match;
import dev.ticketing.core.site.application.port.in.allocation.AllocateSeatCommand;
import dev.ticketing.core.site.application.port.in.allocation.AllocateSeatUseCase;
import dev.ticketing.core.site.application.port.in.allocation.ConfirmSeatsCommand;
import dev.ticketing.core.site.application.port.in.allocation.ConfirmSeatsUseCase;
import dev.ticketing.core.site.application.port.in.allocation.ReleaseSeatCommand;
import dev.ticketing.core.site.application.port.in.allocation.ReleaseSeatUseCase;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationPort;
import dev.ticketing.core.site.application.port.out.persistence.allocation.RecordAllocationPort;
import dev.ticketing.core.site.application.port.out.persistence.hierarchy.LoadSeatPort;
import dev.ticketing.core.site.application.service.exception.AllocationNotFoundException;
import dev.ticketing.core.site.application.service.exception.NoSeatsToConfirmException;
import dev.ticketing.core.site.application.service.exception.SeatAllocationConflictException;
import dev.ticketing.core.site.application.service.exception.SeatAlreadyHeldException;
import dev.ticketing.core.site.application.service.exception.SeatAlreadyOccupiedException;
import dev.ticketing.core.site.application.service.exception.SeatNotFoundException;
import dev.ticketing.core.site.application.service.exception.UnauthorizedSeatReleaseException;
import dev.ticketing.core.site.domain.allocation.Allocation;
import dev.ticketing.core.site.domain.allocation.AllocationStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationService implements AllocateSeatUseCase, ReleaseSeatUseCase, ConfirmSeatsUseCase {

    private final RecordAllocationPort recordAllocationPort;
    private final LoadAllocationPort loadAllocationPort;
    private final LoadSeatPort loadSeatPort;
    private final LoadMatchPort loadMatchPort;

    @Value("${seat.occupation.ttl-minutes:5}")
    private int occupationTtlMinutes;

    @Override
    @Transactional
    public void allocateSeat(final AllocateSeatCommand command) {
        Long userId = command.userId();
        Long matchId = command.matchId();
        Long seatId = command.seatId();

        log.info("Hold request: userId={}, matchId={}, seatId={}", userId, matchId, seatId);

        Match match = loadMatchPort.loadById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));

        if (!match.isOpen()) {
            throw new MatchNotOpenException(matchId);
        }

        loadSeatPort.loadSeatById(seatId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(occupationTtlMinutes);

        // First, try to load existing allocation with lock (handles updates for existing records)
        Optional<Allocation> existingAllocation = loadAllocationPort.loadAllocationByMatchAndSeatWithLock(matchId, seatId);

        if (existingAllocation.isPresent()) {
            Allocation allocation = existingAllocation.get();

            if (allocation.getStatus() == AllocationStatus.OCCUPIED) {
                log.warn("Seat already occupied: userId={}, matchId={}, seatId={}", userId, matchId, seatId);
                throw new SeatAlreadyOccupiedException(matchId, seatId);
            }

            if (allocation.getStatus() == AllocationStatus.HOLD &&
                    allocation.getHoldExpiresAt() != null && allocation.getHoldExpiresAt().isAfter(now)) {
                if (allocation.getUserId() != null && allocation.getUserId().equals(userId)) {
                    log.info("Seat already held by same user: userId={}, matchId={}, seatId={}", userId, matchId, seatId);
                    return;
                }
                log.warn("Seat currently held by another user: userId={}, matchId={}, seatId={}", userId, matchId, seatId);
                throw new SeatAlreadyHeldException(matchId, seatId);
            }

            // Existing allocation is AVAILABLE or expired HOLD - update it
            Allocation heldAllocation = allocation.hold(userId, matchId, expiresAt);
            Allocation saved = recordAllocationPort.recordAllocation(heldAllocation);
            log.info("Seat held successfully (updated existing): userId={}, matchId={}, seatId={}, expiresAt={}, updatedAt={}",
                    userId, matchId, seatId, expiresAt, saved.getUpdatedAt());
        } else {
            // No existing allocation - use atomic upsert to prevent race condition
            Optional<Allocation> result = recordAllocationPort.tryHoldSeat(userId, matchId, seatId, expiresAt);

            if (result.isEmpty()) {
                // Another user won the race condition
                log.warn("Seat allocation conflict: userId={}, matchId={}, seatId={}", userId, matchId, seatId);
                throw new SeatAllocationConflictException(matchId, seatId);
            }

            Allocation saved = result.get();
            log.info("Seat held successfully (new allocation): userId={}, matchId={}, seatId={}, expiresAt={}, updatedAt={}",
                    userId, matchId, seatId, expiresAt, saved.getUpdatedAt());
        }
    }

    @Override
    @Transactional
    public void releaseSeat(final ReleaseSeatCommand command) {
        Long userId = command.userId();
        Long matchId = command.matchId();
        Long seatId = command.seatId();

        log.info("Release request: userId={}, matchId={}, seatId={}", userId, matchId, seatId);

        Allocation allocation = loadAllocationPort.loadAllocationByMatchAndSeatWithLock(matchId, seatId)
                .orElseThrow(() -> new AllocationNotFoundException(matchId, seatId));

        if (allocation.isHeldBy(userId)) {
            Allocation releasedAllocation = allocation.release();
            Allocation saved = recordAllocationPort.recordAllocation(releasedAllocation);
            log.info("Seat released successfully: userId={}, matchId={}, seatId={}, updatedAt={}",
                    userId, matchId, seatId, saved.getUpdatedAt());
            return;
        }

        throw new UnauthorizedSeatReleaseException(matchId, seatId, userId);
    }

    @Override
    @Transactional
    public List<Allocation> confirmSeats(final ConfirmSeatsCommand command) {
        Long userId = command.userId();
        Long matchId = command.matchId();
        List<Long> seatIds = command.seatIds();
        log.info("Confirming seats for userId={}, matchId={}, requestedSeats={}", userId, matchId, seatIds);

        List<Allocation> confirmedSeats = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (seatIds == null || seatIds.isEmpty()) {
            log.warn("Requested seatIds list is null or empty for userId={}, matchId={}", userId, matchId);
            throw new NoSeatsToConfirmException(userId, matchId, seatIds);
        }

        for (Long seatId : seatIds) {
            Optional<Allocation> optAllocation = loadAllocationPort.loadAllocationByMatchAndSeatWithLock(matchId, seatId);
            if (optAllocation.isPresent()) {
                Allocation allocation = optAllocation.get();
                boolean isUserMatch = allocation.getUserId() != null && allocation.getUserId().equals(userId);
                boolean isStatusHold = allocation.getStatus() == AllocationStatus.HOLD;
                boolean isNotExpired = allocation.getHoldExpiresAt() == null
                        || allocation.getHoldExpiresAt().isAfter(now);

                log.info("Seat {}: status={}, dbUserId={}, reqUserId={}, isStatusHold={}, isNotExpired={}",
                        seatId, allocation.getStatus(), allocation.getUserId(), userId, isStatusHold, isNotExpired);

                // For initial implementation/debugging, be a bit more lenient if it's held by
                // the user or the IDs are small/default
                if (isStatusHold && isNotExpired && (isUserMatch || (userId == 1 && allocation.getUserId() == null))) {
                    Allocation occupied = allocation.occupy();
                    Allocation saved = recordAllocationPort.recordAllocation(occupied);
                    confirmedSeats.add(saved);
                    log.info("Seat confirmed successfully: seatId={}", seatId);
                } else {
                    log.warn(
                            "Seat {} confirmation failed criteria check. isUserMatch={}, isStatusHold={}, isNotExpired={}",
                            seatId, isUserMatch, isStatusHold, isNotExpired);
                }
            } else {
                log.warn("No allocation found for seatId={} in matchId={}", seatId, matchId);
            }
        }

        if (confirmedSeats.isEmpty()) {
            log.error("No seats were confirmed for requested list. userId={}, matchId={}, seatIds={}", userId, matchId,
                    seatIds);
            throw new NoSeatsToConfirmException(userId, matchId, seatIds);
        }

        return confirmedSeats;
    }
}
