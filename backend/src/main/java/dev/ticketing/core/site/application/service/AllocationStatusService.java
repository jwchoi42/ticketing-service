package dev.ticketing.core.site.application.service;

import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusChangesUseCase;
import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusSnapShotUseCase;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationStatusPort;
import dev.ticketing.core.site.domain.allocation.AllocationStatus;
import dev.ticketing.core.site.domain.allocation.AllocationStatusSnapShot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * AllocationStatusQueryService - 좌석 현황 조회 전용 서비스
 *
 * Request Collapsing 적용:
 * - 동시에 같은 matchId:blockId로 요청이 들어오면 DB 쿼리 1번만 실행
 * - 캐시와 달리 데이터를 저장하지 않아 일관성 문제 없음
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllocationStatusService
        implements GetAllocationStatusSnapShotUseCase, GetAllocationStatusChangesUseCase {

    private static final long TIMEOUT_SECONDS = 5;

    private final LoadAllocationStatusPort loadAllocationStatusPort;

    // 진행 중인 스냅샷 조회 요청 추적
    private final Map<String, CompletableFuture<AllocationStatusSnapShot>> inFlightSnapshots
            = new ConcurrentHashMap<>();

    @Override
    public AllocationStatusSnapShot getAllocationStatusSnapShotByMatchIdAndBlockId(Long matchId, Long blockId) {
        String key = matchId + ":" + blockId;

        CompletableFuture<AllocationStatusSnapShot> future = inFlightSnapshots.computeIfAbsent(key, k ->
                CompletableFuture
                        .supplyAsync(() -> loadAllocationStatusPort
                                .loadAllocationStatusSnapShotByMatchIdAndBlockId(matchId, blockId))
                        .whenComplete((result, ex) -> inFlightSnapshots.remove(key))
        );

        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.error("AllocationStatusSnapShot 조회 타임아웃: matchId={}, blockId={}", matchId, blockId);
            throw new RuntimeException("조회 타임아웃", e);
        } catch (Exception e) {
            log.error("AllocationStatusSnapShot 조회 실패: matchId={}, blockId={}", matchId, blockId, e);
            throw new RuntimeException("조회 실패", e);
        }
    }

    @Override
    public List<AllocationStatus> getAllocationChangesSince(Long matchId, Long blockId, LocalDateTime since) {
        return loadAllocationStatusPort.loadAllocationStatusesSince(matchId, blockId, since);
    }
}
