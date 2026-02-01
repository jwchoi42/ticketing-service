package dev.ticketing.core.site.application.port.out.persistence.allocation;

import dev.ticketing.core.site.domain.allocation.AllocationStatus;
import dev.ticketing.core.site.domain.allocation.AllocationStatusSnapShot;

import java.time.LocalDateTime;
import java.util.List;

public interface LoadAllocationStatusPort {

    AllocationStatusSnapShot loadAllocationStatusSnapShotByMatchIdAndBlockId(Long matchId, Long blockId);

    /**
     * 정규화 시뮬레이션용 - seat 테이블과 JOIN하여 조회
     */
    AllocationStatusSnapShot loadAllocationStatusSnapShotByMatchIdAndBlockIdWithJoin(Long matchId, Long blockId);

    /**
     * 특정 좌석의 상태 조회
     *
     * @param matchId 경기 ID
     * @param seatId  좌석 ID
     * @return 좌석 상태 (없으면 AVAILABLE)
     */
    AllocationStatus status(Long matchId, Long seatId);

    /**
     * 특정 시간 이후 변경된 좌석 상태만 조회 (서버 폴링용)
     *
     * @param matchId 경기 ID
     * @param blockId 구간 ID
     * @param since   마지막 확인 시간
     * @return 변경된 좌석 상태 리스트
     */
    List<AllocationStatus> loadAllocationStatusesSince(Long matchId, Long blockId, LocalDateTime since);
}
