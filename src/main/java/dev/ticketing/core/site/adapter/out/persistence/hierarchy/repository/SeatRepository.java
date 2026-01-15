package dev.ticketing.core.site.adapter.out.persistence.hierarchy.repository;

import dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.SeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import java.util.Optional;

public interface SeatRepository extends JpaRepository<SeatEntity, Long> {
    List<SeatEntity> findByBlockId(Long blockId);

    Optional<SeatEntity> findByBlockIdAndRowNumberAndSeatNumber(Long blockId, Integer rowNumber, Integer seatNumber);
}
