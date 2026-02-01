package dev.ticketing.core.site.application.service;

import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusChangesUseCase;
import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusSnapShotUseCase;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationStatusPort;
import dev.ticketing.core.site.domain.allocation.AllocationStatus;
import dev.ticketing.core.site.domain.allocation.AllocationStatusSnapShot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final Map<String, CompletableFuture<AllocationStatusSnapShot>> inFlightSnapshots
            = new ConcurrentHashMap<>();

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
            case "none" -> loadFromDb(matchId, blockId, normalized);
            case "collapsing" -> loadWithCollapsing(matchId, blockId, normalized);
            case "redis" -> loadWithCache(matchId, blockId, redisCacheManager, normalized);
            case "caffeine" -> loadWithCache(matchId, blockId, caffeineCacheManager, normalized);
            default -> loadWithCollapsing(matchId, blockId, normalized);
        };
    }

    // ===== 전략별 구현 =====

    /**
     * 전략 1: 캐시 없음 (기준선)
     */
    private AllocationStatusSnapShot loadFromDb(Long matchId, Long blockId, boolean normalized) {
        if (normalized) {
            return loadAllocationStatusPort.loadAllocationStatusSnapShotByMatchIdAndBlockIdWithJoin(matchId, blockId);
        }
        return loadAllocationStatusPort.loadAllocationStatusSnapShotByMatchIdAndBlockId(matchId, blockId);
    }

    /**
     * 전략 2: Request Collapsing (동기 방식)
     * - 동시 요청은 같은 Future를 공유하여 DB 쿼리 1번만 실행
     * - 첫 번째 요청 스레드가 직접 실행하고, 나머지는 결과를 기다림
     */
    private AllocationStatusSnapShot loadWithCollapsing(Long matchId, Long blockId, boolean normalized) {
        String key = matchId + ":" + blockId + ":" + (normalized ? "n" : "d");

        // 이미 진행 중인 요청이 있으면 그 결과를 기다림
        CompletableFuture<AllocationStatusSnapShot> existing = inFlightSnapshots.get(key);
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
            AllocationStatusSnapShot result = loadFromDb(matchId, blockId, normalized);
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

    /**
     * 전략 3, 4: Cache (Redis 또는 Caffeine)
     */
    private AllocationStatusSnapShot loadWithCache(Long matchId, Long blockId, CacheManager cacheManager, boolean normalized) {
        String key = matchId + ":" + blockId + ":" + (normalized ? "n" : "d");
        Cache cache = cacheManager.getCache(CACHE_NAME);

        if (cache != null) {
            AllocationStatusSnapShot cached = cache.get(key, AllocationStatusSnapShot.class);
            if (cached != null) {
                return cached;
            }
        }

        AllocationStatusSnapShot result = loadFromDb(matchId, blockId, normalized);

        if (cache != null) {
            cache.put(key, result);
        }

        return result;
    }

    @Override
    public List<AllocationStatus> getAllocationChangesSince(Long matchId, Long blockId, LocalDateTime since) {
        return loadAllocationStatusPort.loadAllocationStatusesSince(matchId, blockId, since);
    }
}
