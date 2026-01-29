package dev.ticketing.core.site.application.service;

import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusChangesUseCase;
import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusSnapShotUseCase;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationStatusPort;
import dev.ticketing.core.site.domain.allocation.AllocationStatus;
import dev.ticketing.core.site.domain.allocation.AllocationStatusSnapShot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
 * AllocationStatusService - 좌석 현황 조회 서비스
 *
 * 부하 테스트를 위해 여러 전략을 지원:
 * - none: 캐시 없이 매번 DB 조회 (기준선)
 * - collapsing: Request Collapsing (동시 요청 합치기)
 * - redis: Redis Cache
 * - caffeine: Caffeine (로컬) Cache
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllocationStatusService
        implements GetAllocationStatusSnapShotUseCase, GetAllocationStatusChangesUseCase {

    private static final long TIMEOUT_SECONDS = 5;
    private static final String CACHE_NAME = "allocationStatusSnapShot";

    private final LoadAllocationStatusPort loadAllocationStatusPort;
    private final CacheManager redisCacheManager;
    private final CacheManager caffeineCacheManager;

    // Request Collapsing용
    private final Map<String, CompletableFuture<AllocationStatusSnapShot>> inFlightSnapshots
            = new ConcurrentHashMap<>();

    @Override
    public AllocationStatusSnapShot getAllocationStatusSnapShotByMatchIdAndBlockId(Long matchId, Long blockId) {
        return getAllocationStatusSnapShotByMatchIdAndBlockId(matchId, blockId, "collapsing");
    }

    public AllocationStatusSnapShot getAllocationStatusSnapShotByMatchIdAndBlockId(
            Long matchId, Long blockId, String strategy) {

        return switch (strategy) {
            case "none" -> loadFromDb(matchId, blockId);
            case "collapsing" -> loadWithCollapsing(matchId, blockId);
            case "redis" -> loadWithCache(matchId, blockId, redisCacheManager);
            case "caffeine" -> loadWithCache(matchId, blockId, caffeineCacheManager);
            default -> loadWithCollapsing(matchId, blockId);
        };
    }

    // ===== 전략별 구현 =====

    /**
     * 전략 1: 캐시 없음 (기준선)
     */
    private AllocationStatusSnapShot loadFromDb(Long matchId, Long blockId) {
        return loadAllocationStatusPort.loadAllocationStatusSnapShotByMatchIdAndBlockId(matchId, blockId);
    }

    /**
     * 전략 2: Request Collapsing
     */
    private AllocationStatusSnapShot loadWithCollapsing(Long matchId, Long blockId) {
        String key = matchId + ":" + blockId;

        CompletableFuture<AllocationStatusSnapShot> existing = inFlightSnapshots.get(key);

        if (existing != null) {
            return waitForResult(existing, matchId, blockId);
        }

        CompletableFuture<AllocationStatusSnapShot> newFuture = new CompletableFuture<>();
        CompletableFuture<AllocationStatusSnapShot> registered = inFlightSnapshots.putIfAbsent(key, newFuture);

        if (registered != null) {
            return waitForResult(registered, matchId, blockId);
        }

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

    /**
     * 전략 3, 4: Cache (Redis 또는 Caffeine)
     */
    private AllocationStatusSnapShot loadWithCache(Long matchId, Long blockId, CacheManager cacheManager) {
        String key = matchId + ":" + blockId;
        Cache cache = cacheManager.getCache(CACHE_NAME);

        if (cache != null) {
            AllocationStatusSnapShot cached = cache.get(key, AllocationStatusSnapShot.class);
            if (cached != null) {
                return cached;
            }
        }

        AllocationStatusSnapShot result = loadAllocationStatusPort
                .loadAllocationStatusSnapShotByMatchIdAndBlockId(matchId, blockId);

        if (cache != null) {
            cache.put(key, result);
        }

        return result;
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
