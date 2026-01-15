package dev.ticketing.core.site.application.port.out.persistence.allocation;

import dev.ticketing.core.site.domain.allocation.Allocation;

import java.time.LocalDateTime;
import java.util.List;

public interface LoadAllocationStatusPort {

    /**
     * 특정 구간의 모든 좌석 상태 조회
     *
     * @param matchId 경기 ID
     * @param blockId 구간 ID
     * @return 좌석 ID와 상태 리스트
     */
    List<Allocation> loadAllocationStatusesByBlockId(Long matchId, Long blockId);

    /**
     * 특정 좌석의 상태 조회
     *
     * @param matchId 경기 ID
     * @param seatId  좌석 ID
     * @return 좌석 상태 (없으면 AVAILABLE)
     */
    Allocation loadAllocationStatus(Long matchId, Long seatId);

    /**
     * 특정 시간 이후 변경된 좌석 상태만 조회 (서버 폴링용)
     *
     * @param matchId 경기 ID
     * @param blockId 구간 ID
     * @param since   마지막 확인 시간
     * @return 변경된 좌석 상태 리스트
     */
    List<Allocation> loadAllocationStatusesSince(Long matchId, Long blockId, LocalDateTime since);
}
