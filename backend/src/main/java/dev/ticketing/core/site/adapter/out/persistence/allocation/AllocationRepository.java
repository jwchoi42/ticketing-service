package dev.ticketing.core.site.adapter.out.persistence.allocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AllocationRepository extends JpaRepository<AllocationEntity, Long> {
    List<AllocationEntity> findByReservationId(Long reservationId);

    Optional<AllocationEntity> findByMatchIdAndSeatId(Long matchId, Long seatId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AllocationEntity a WHERE a.matchId = :matchId AND a.seatId = :seatId")
    Optional<AllocationEntity> findByMatchIdAndSeatIdWithLock(@Param("matchId") Long matchId,
            @Param("seatId") Long seatId);

    /**
     * 특정 경기의 좌석 ID 목록에 해당하는 allocations 조회
     */
    List<AllocationEntity> findByMatchIdAndSeatIdIn(Long matchId, List<Long> seatIds);

    /**
     * 특정 시간 이후 변경된 allocations 조회 (서버 폴링용)
     */
    @Query("SELECT a FROM AllocationEntity a WHERE a.matchId = :matchId AND a.seatId IN :seatIds AND a.updatedAt > :updatedAt")
    List<AllocationEntity> findByMatchIdAndSeatIdInAndUpdatedAtAfter(
            @Param("matchId") Long matchId,
            @Param("seatIds") List<Long> seatIds,
            @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Atomic upsert for seat allocation to prevent race conditions.
     * Uses PostgreSQL INSERT ON CONFLICT to atomically insert or update.
     * Only succeeds if the seat is AVAILABLE or has an expired HOLD.
     * Returns number of affected rows (always 1 due to upsert behavior).
     */
    @Modifying
    @Query(value = """
            INSERT INTO allocations (user_id, match_id, seat_id, status, hold_expires_at, updated_at)
            VALUES (:userId, :matchId, :seatId, 'HOLD', :holdExpiresAt, NOW())
            ON CONFLICT (match_id, seat_id) DO UPDATE
            SET user_id = CASE
                WHEN allocations.status = 'AVAILABLE'
                     OR (allocations.status = 'HOLD' AND allocations.hold_expires_at < NOW())
                THEN EXCLUDED.user_id
                ELSE allocations.user_id
            END,
            status = CASE
                WHEN allocations.status = 'AVAILABLE'
                     OR (allocations.status = 'HOLD' AND allocations.hold_expires_at < NOW())
                THEN 'HOLD'
                ELSE allocations.status
            END,
            hold_expires_at = CASE
                WHEN allocations.status = 'AVAILABLE'
                     OR (allocations.status = 'HOLD' AND allocations.hold_expires_at < NOW())
                THEN EXCLUDED.hold_expires_at
                ELSE allocations.hold_expires_at
            END,
            updated_at = NOW()
            """, nativeQuery = true)
    int tryHoldSeat(
            @Param("userId") Long userId,
            @Param("matchId") Long matchId,
            @Param("seatId") Long seatId,
            @Param("holdExpiresAt") LocalDateTime holdExpiresAt
    );
}
