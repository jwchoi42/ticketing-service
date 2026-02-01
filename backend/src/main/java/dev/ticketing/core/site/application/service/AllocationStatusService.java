package dev.ticketing.core.site.application.service;

import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusChangesUseCase;
import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusSnapShotUseCase;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationStatusPort;
import dev.ticketing.core.site.domain.allocation.AllocationStatus;
import dev.ticketing.core.site.domain.allocation.AllocationStatusSnapShot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * AllocationStatusService - 좌석 현황 조회 서비스
 *
 * 부하 테스트를 위해 여러 전략을 지원:
 * - strategy: none, collapsing, redis, caffeine
 * - schema: normalized (JOIN 쿼리), denormalized (비정규화 쿼리)
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class AllocationStatusService
        implements GetAllocationStatusSnapShotUseCase, GetAllocationStatusChangesUseCase {

    private static final long TIMEOUT_SECONDS = 5;
    private static final String CACHE_NAME = "allocationStatusSnapShot";

    private final LoadAllocationStatusPort loadAllocationStatusPort;
    private final CacheManager redisCacheManager;
    private final CacheManager caffeineCacheManager;

    // Request Collapsing용 - 진행 중인 요청 추적
    private final Map<String, CompletableFuture<AllocationStatusSnapShot>> inFlightSnapshots = new ConcurrentHashMap<>();

    public AllocationStatusService(
            LoadAllocationStatusPort loadAllocationStatusPort,
            @Qualifier("redisCacheManager") CacheManager redisCacheManager,
            @Qualifier("caffeineCacheManager") CacheManager caffeineCacheManager) {
        this.loadAllocationStatusPort = loadAllocationStatusPort;
        this.redisCacheManager = redisCacheManager;
        this.caffeineCacheManager = caffeineCacheManager;
    }

    @Override
    public AllocationStatusSnapShot getAllocationStatusSnapShotByMatchIdAndBlockId(Long matchId, Long blockId) {
        return getAllocationStatusSnapShotByMatchIdAndBlockId(matchId, blockId, "collapsing", "denormalized");
    }

    public AllocationStatusSnapShot getAllocationStatusSnapShotByMatchIdAndBlockId(
            Long matchId, Long blockId, String strategy, String schema) {

        boolean normalized = "normalized".equals(schema);

        return switch (strategy) {
            case "none" -> normalized ? loadFromDbWithJoin(matchId, blockId) : loadFromDb(matchId, blockId);
            case "collapsing" -> loadWithCollapsing(matchId, blockId, normalized);
            case "redis" -> loadWithCache(matchId, blockId, redisCacheManager, normalized);
            case "caffeine" -> loadWithCache(matchId, blockId, caffeineCacheManager, normalized);
            default -> loadWithCollapsing(matchId, blockId, normalized);
        };
    }

    // ===== 전략별 구현 =====

    /**
     * 비정규화 쿼리 (block_id 직접 조회)
     */
    private AllocationStatusSnapShot loadFromDb(Long matchId, Long blockId) {
        return loadAllocationStatusPort.loadAllocationStatusSnapShotByMatchIdAndBlockId(matchId, blockId);
    }

    /**
     * 정규화 쿼리 (seat JOIN 필요)
     */
    private AllocationStatusSnapShot loadFromDbWithJoin(Long matchId, Long blockId) {
        return loadAllocationStatusPort.loadAllocationStatusSnapShotByMatchIdAndBlockIdWithJoin(matchId, blockId);
    }

    private AllocationStatusSnapShot loadFromDb(Long matchId, Long blockId, boolean normalized) {
        return normalized ? loadFromDbWithJoin(matchId, blockId) : loadFromDb(matchId, blockId);
    }

    /**
     * Request Collapsing (동기 방식)
     */
    private AllocationStatusSnapShot loadWithCollapsing(Long matchId, Long blockId, boolean normalized) {
        String key = matchId + ":" + blockId + ":" + (normalized ? "n" : "d");

        return Optional.ofNullable(inFlightSnapshots.get(key))
                .map(this::awaitFuture)
                .orElseGet(() -> executeWithCollapsing(key, matchId, blockId, normalized));
    }

    private AllocationStatusSnapShot executeWithCollapsing(String key, Long matchId, Long blockId, boolean normalized) {
        CompletableFuture<AllocationStatusSnapShot> newFuture = new CompletableFuture<>();

        return Optional.ofNullable(inFlightSnapshots.putIfAbsent(key, newFuture))
                .map(this::awaitFuture)
                .orElseGet(() -> {
                    try {
                        AllocationStatusSnapShot result = loadFromDb(matchId, blockId, normalized);
                        newFuture.complete(result);
                        return result;
                    } catch (Exception e) {
                        newFuture.completeExceptionally(e);
                        throw e;
                    } finally {
                        inFlightSnapshots.remove(key);
                    }
                });
    }

    private AllocationStatusSnapShot awaitFuture(CompletableFuture<AllocationStatusSnapShot> future) {
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("좌석 현황 조회 타임아웃", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("좌석 현황 조회 실패", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("좌석 현황 조회 중단", e);
        }
    }

    /**
     * Cache (Redis 또는 Caffeine)
     */
    private AllocationStatusSnapShot loadWithCache(Long matchId, Long blockId, CacheManager cacheManager, boolean normalized) {
        String key = matchId + ":" + blockId + ":" + (normalized ? "n" : "d");

        return Optional.ofNullable(cacheManager.getCache(CACHE_NAME))
                .map(cache -> cache.get(key, () -> loadFromDb(matchId, blockId, normalized)))
                .orElseGet(() -> loadFromDb(matchId, blockId, normalized));
    }

    @Override
    public List<AllocationStatus> getAllocationChangesSince(Long matchId, Long blockId, LocalDateTime since) {
        return loadAllocationStatusPort.loadAllocationStatusesSince(matchId, blockId, since);
    }
}
