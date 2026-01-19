package dev.ticketing.core.site.application.port.out.persistence.allocation;

import dev.ticketing.core.site.domain.allocation.Allocation;

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
}
