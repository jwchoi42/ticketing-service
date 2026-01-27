package dev.ticketing.core.site.adapter.out.persistence.allocation;

import dev.ticketing.core.site.domain.allocation.AllocationStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface AllocationRepositoryQuery {

    List<AllocationStatus> findAllocationStatusesByMatchIdAndBlockId(Long matchId, Long blockId);

    List<AllocationStatus> findAllocationStatusesByBlockIdAndUpdatedAtAfter(
            Long matchId, Long blockId, LocalDateTime since);

}
