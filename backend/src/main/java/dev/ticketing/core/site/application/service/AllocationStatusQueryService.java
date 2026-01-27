package dev.ticketing.core.site.application.service;

import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusChangesUseCase;
import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusSnapShotUseCase;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationStatusPort;
import dev.ticketing.core.site.domain.allocation.Allocation;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AllocationStatusQueryService - 좌석 현황 조회 전용 서비스
 * 기술 중립적: SSE, Scheduler 등 인프라 관련 코드 없이 순수 비즈니스 로직만 담당
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllocationStatusQueryService
        implements GetAllocationStatusSnapShotUseCase, GetAllocationStatusChangesUseCase {

    private final LoadAllocationStatusPort loadAllocationStatusPort;

    @Override
    @Cacheable(value = "allocationSnapshot", key = "#matchId + ':' + #blockId")
    public List<Allocation> getAllocationSnapshot(Long matchId, Long blockId) {
        return loadAllocationStatusPort.loadAllocationStatusesByBlockId(matchId, blockId);
    }

    @Override
    public List<Allocation> getAllocationChangesSince(Long matchId, Long blockId, LocalDateTime since) {
        return loadAllocationStatusPort.loadAllocationStatusesSince(matchId, blockId, since);
    }
}
