package dev.ticketing.core.site.adapter.out.persistence.allocation;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
