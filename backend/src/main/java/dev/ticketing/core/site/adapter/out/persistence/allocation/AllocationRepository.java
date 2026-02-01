package dev.ticketing.core.site.adapter.out.persistence.allocation;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AllocationRepository extends JpaRepository<AllocationEntity, Long>, AllocationRepositoryQuery {

    List<AllocationEntity> findByReservationId(Long reservationId);

    Optional<AllocationEntity> findByMatchIdAndSeatId(Long matchId, Long seatId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AllocationEntity a WHERE a.match.id = :matchId AND a.seat.id = :seatId")
    Optional<AllocationEntity> findByMatchIdAndSeatIdWithLock(@Param("matchId") Long matchId,
            @Param("seatId") Long seatId);

}
