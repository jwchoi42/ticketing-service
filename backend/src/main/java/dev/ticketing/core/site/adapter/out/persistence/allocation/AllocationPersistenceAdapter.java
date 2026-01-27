package dev.ticketing.core.site.adapter.out.persistence.allocation;

import dev.ticketing.core.site.adapter.out.persistence.hierarchy.repository.SeatRepository;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationPort;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationStatusPort;
import dev.ticketing.core.site.application.port.out.persistence.allocation.RecordAllocationPort;
import dev.ticketing.core.site.domain.allocation.Allocation;
import dev.ticketing.core.site.domain.allocation.AllocationStatus;
import dev.ticketing.core.site.domain.allocation.AllocationStatusSnapShot;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Primary
@Component
@RequiredArgsConstructor
public class AllocationPersistenceAdapter
        implements RecordAllocationPort, LoadAllocationPort, LoadAllocationStatusPort {

    private final AllocationRepository allocationRepository;
    private final SeatRepository seatRepository;

    // --- Load Allocation Status ---

    // Allocation Status Snap Shot

    @Override
    public AllocationStatusSnapShot loadAllocationStatusSnapShotByMatchIdAndBlockId(Long matchId, Long blockId) {
        List<AllocationStatus> allocationStatuses
                = allocationRepository.findAllocationStatusesByMatchIdAndBlockId(matchId, blockId);
        return AllocationStatusSnapShot.from(allocationStatuses);
    }

    // --- RecordAllocationPort ---

    @Override
    public Allocation recordAllocation(Allocation allocation) {
        return allocationRepository.save(AllocationEntity.from(allocation)).toDomain();
    }

    @Override
    public void saveAll(List<Allocation> allocations) {
        List<AllocationEntity> entities = allocations.stream()
                .map(AllocationEntity::from)
                .toList();
        allocationRepository.saveAll(entities);
    }

    // --- LoadAllocationPort ---

    @Override
    public Optional<Allocation> loadAllocationByMatchAndSeatWithLock(Long matchId, Long seatId) {
        return allocationRepository.findByMatchIdAndSeatIdWithLock(matchId, seatId).map(AllocationEntity::toDomain);
    }

    @Override
    public List<Allocation> loadAllocationsByReservationId(Long reservationId) {
        return allocationRepository.findByReservationId(reservationId).stream()
                .map(AllocationEntity::toDomain)
                .toList();
    }

    @Override
    public AllocationStatus status(Long matchId, Long seatId) {
        return allocationRepository.findByMatchIdAndSeatId(matchId, seatId)
                .map(e -> AllocationStatus.from(e.toDomain()))
                .orElseGet(() -> {
                    Long blockId = seatRepository.findById(seatId)
                            .map(seat -> seat.getBlockId())
                            .orElse(null);
                    return AllocationStatus.from(Allocation.availableForMatch(matchId, blockId, seatId));
                });
    }

    @Override
    public List<AllocationStatus> loadAllocationStatusesSince(Long matchId, Long blockId, LocalDateTime since) {
        return allocationRepository.findAllocationStatusesByBlockIdAndUpdatedAtAfter(matchId, blockId, since);
    }
}
