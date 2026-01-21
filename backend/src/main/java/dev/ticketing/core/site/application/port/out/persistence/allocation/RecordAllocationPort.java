package dev.ticketing.core.site.application.port.out.persistence.allocation;

import dev.ticketing.core.site.domain.allocation.Allocation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 좌석 할당 기록 Port
 */
public interface RecordAllocationPort {

    /**
     * 좌석 할당을 기록한다
     *
     * @param allocation 좌석 할당 정보
     * @return 저장된 좌석 할당
     */
    Allocation recordAllocation(Allocation allocation);

    /**
     * 여러 좌석 할당을 한번에 기록한다
     *
     * @param allocations 좌석 할당 정보 목록
     */
    void saveAll(List<Allocation> allocations);

    /**
     * Atomically attempt to hold a seat.
     * Uses INSERT ON CONFLICT to prevent race conditions.
     * Returns the allocation only if the current user successfully acquired the hold.
     *
     * @param userId        the user attempting to hold the seat
     * @param matchId       the match ID
     * @param seatId        the seat ID
     * @param holdExpiresAt when the hold expires
     * @return Optional containing the allocation if successful, empty if seat was already held
     */
    Optional<Allocation> tryHoldSeat(Long userId, Long matchId, Long seatId, LocalDateTime holdExpiresAt);
}
