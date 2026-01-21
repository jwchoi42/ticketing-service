package dev.ticketing.core.site.adapter.out.persistence.allocation;

import dev.ticketing.core.site.adapter.out.persistence.hierarchy.entity.SeatEntity;
import dev.ticketing.core.site.adapter.out.persistence.hierarchy.repository.SeatRepository;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationPort;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationStatusPort;
import dev.ticketing.core.site.application.port.out.persistence.allocation.RecordAllocationPort;
import dev.ticketing.core.site.domain.allocation.Allocation;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Primary
@Component
@RequiredArgsConstructor
public class AllocationPersistenceAdapter
        implements RecordAllocationPort, LoadAllocationPort, LoadAllocationStatusPort {

    private final AllocationRepository allocationRepository;
    private final SeatRepository seatRepository;

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

    // --- LoadAllocationStatusPort ---

    @Override
    public List<Allocation> loadAllocationStatusesByBlockId(Long matchId, Long blockId) {
        // 1. 해당 구간의 좌석 목록 조회
        List<SeatEntity> seats = seatRepository.findByBlockId(blockId);

        if (seats.isEmpty()) {
            return List.of();
        }

        List<Long> seatIds = seats.stream().map(SeatEntity::getId).toList();

        // 2. allocations 테이블 조회
        List<Allocation> existingAllocations = allocationRepository.findByMatchIdAndSeatIdIn(matchId, seatIds)
                .stream()
                .map(AllocationEntity::toDomain)
                .toList();

        java.util.Map<Long, Allocation> allocationMap = existingAllocations.stream()
                .collect(Collectors.toMap(Allocation::getSeatId, a -> a));

        // 3. 모든 좌석에 대해 상태 보장 (없으면 AVAILABLE)
        return seatIds.stream()
                .map(seatId -> allocationMap.getOrDefault(seatId, Allocation.available(seatId)))
                .collect(Collectors.toList());
    }

    @Override
    public Allocation loadAllocationStatus(Long matchId, Long seatId) {
        return allocationRepository.findByMatchIdAndSeatId(matchId, seatId)
                .map(AllocationEntity::toDomain)
                .orElse(Allocation.available(seatId));
    }

    @Override
    public List<Allocation> loadAllocationStatusesSince(Long matchId, Long blockId, LocalDateTime since) {
        // 1. 해당 구간의 좌석 ID 목록 조회
        List<Long> seatIds = seatRepository.findByBlockId(blockId)
                .stream()
                .map(SeatEntity::getId)
                .collect(Collectors.toList());

        if (seatIds.isEmpty()) {
            return List.of();
        }

        // 2. 변경된 allocations만 조회
        return allocationRepository.findByMatchIdAndSeatIdInAndUpdatedAtAfter(matchId, seatIds, since)
                .stream()
                .map(AllocationEntity::toDomain)
                .collect(Collectors.toList());
    }
}
