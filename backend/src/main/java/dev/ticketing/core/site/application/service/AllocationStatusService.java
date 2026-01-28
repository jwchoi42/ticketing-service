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
 * - 첫 번째 요청 스레드가 직접 실행하고, 나머지는 결과를 기다림
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

        CompletableFuture<AllocationStatusSnapShot> existing = inFlightSnapshots.get(key);

        // 이미 진행 중인 요청이 있으면 그 결과를 기다림
        if (existing != null) {
            return waitForResult(existing, matchId, blockId);
        }

        // 새로운 Future 생성 및 등록 시도
        CompletableFuture<AllocationStatusSnapShot> newFuture = new CompletableFuture<>();
        CompletableFuture<AllocationStatusSnapShot> registered = inFlightSnapshots.putIfAbsent(key, newFuture);

        // 다른 스레드가 먼저 등록했으면 그 결과를 기다림
        if (registered != null) {
            return waitForResult(registered, matchId, blockId);
        }

        // 첫 번째 스레드: 직접 실행
        try {
            AllocationStatusSnapShot result = loadAllocationStatusPort
                    .loadAllocationStatusSnapShotByMatchIdAndBlockId(matchId, blockId);
            newFuture.complete(result);
            return result;
        } catch (Exception e) {
            newFuture.completeExceptionally(e);
            throw e;
        } finally {
            inFlightSnapshots.remove(key);
        }
    }

    private AllocationStatusSnapShot waitForResult(
            CompletableFuture<AllocationStatusSnapShot> future, Long matchId, Long blockId) {
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
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
