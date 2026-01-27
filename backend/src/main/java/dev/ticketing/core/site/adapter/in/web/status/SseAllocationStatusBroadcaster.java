package dev.ticketing.core.site.adapter.in.web.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ticketing.common.web.model.response.SuccessResponse;
import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusChangesUseCase;
import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusSnapShotUseCase;
import dev.ticketing.core.site.domain.allocation.Allocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SseAllocationStatusBroadcaster - SSE 기반 좌석 현황 브로드캐스터 (Adapter)
 * 기술 의존적 코드(SSE, Scheduler)를 Adapter 레이어에서 담당
 * 향후 Redis Stream 전환 시 이 Adapter만 교체하면 됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseAllocationStatusBroadcaster {

    private final GetAllocationStatusSnapShotUseCase snapshotUseCase;
    private final GetAllocationStatusChangesUseCase changesUseCase;
    private final ObjectMapper objectMapper;

    // matchId:blockId -> List<SseEmitter>
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // 마지막 확인 시간 (충분히 과거 시간으로 초기화하여 첫 번째 변경사항도 감지)
    private LocalDateTime lastCheckTime = LocalDateTime.now().minusMinutes(10);

    /**
     * SSE 스트림 구독
     */
    public SseEmitter subscribe(Long matchId, Long blockId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 무제한 타임아웃
        String key = buildKey(matchId, blockId);

        // 1. SSE 연결 등록
        emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("SSE 연결 등록: matchId={}, blockId={}, 총 연결 수={}",
                matchId, blockId, emitters.get(key).size());

        try {
            // 2. 초기 데이터 전송
            List<Allocation> snapshot = snapshotUseCase.getAllocationSnapshot(matchId, blockId);
            sendSnapshotEvent(emitter, snapshot);
            log.info("초기 데이터 전송 완료: matchId={}, blockId={}, 좌석 수={}",
                    matchId, blockId, snapshot.size());

        } catch (Exception e) {
            log.error("초기 데이터 전송 실패", e);
            if (emitters.get(key) != null) {
                emitters.get(key).remove(emitter);
            }
            emitter.completeWithError(e);
            return emitter;
        }

        // 3. 연결 종료 시 제거
        emitter.onCompletion(() -> {
            emitters.get(key).remove(emitter);
            log.info("SSE 연결 종료: matchId={}, blockId={}, 남은 연결 수={}",
                    matchId, blockId, emitters.get(key).size());
        });

        emitter.onTimeout(() -> {
            emitters.get(key).remove(emitter);
            log.warn("SSE 연결 타임아웃: matchId={}, blockId={}", matchId, blockId);
        });

        emitter.onError(e -> {
            emitters.get(key).remove(emitter);
            log.error("SSE 연결 에러: matchId={}, blockId={}", matchId, blockId, e);
        });

        return emitter;
    }

    /**
     * 1초마다 실행: 변경 사항 감지 및 SSE 전송
     */
    @Scheduled(fixedRate = 1000)
    public void pollAndBroadcast() {
        log.debug("스케줄러 실행됨: emitters 수={}", emitters.size());
        if (emitters.isEmpty()) {
            log.debug("연결된 클라이언트 없음 - 스킵");
            return;
        }

        LocalDateTime currentCheckTime = LocalDateTime.now();

        log.info("변경 사항 체크 시작: 연결된 구간 수={}, 마지막 확인 시간={}, 현재 시간={}",
                emitters.size(), lastCheckTime, currentCheckTime);

        emitters.forEach((key, emitterList) -> {
            if (emitterList.isEmpty()) {
                return;
            }

            try {
                Long[] ids = parseKey(key);
                Long matchId = ids[0];
                Long blockId = ids[1];

                // UseCase를 통해 변경 사항 조회
                log.debug("변경 사항 조회 시작: matchId={}, blockId={}, since={}", matchId, blockId, lastCheckTime);
                List<Allocation> changes = changesUseCase.getAllocationChangesSince(matchId, blockId, lastCheckTime);
                log.debug("변경 사항 조회 완료: matchId={}, blockId={}, 변경 수={}", matchId, blockId, changes.size());

                if (!changes.isEmpty()) {
                    log.info("변경 사항 감지: matchId={}, blockId={}, 변경 수={}, 변경 목록={}",
                            matchId, blockId, changes.size(), changes);

                    // 변경분을 한 번의 이벤트로 전송 (Batch)
                    broadcastChanges(emitterList, changes);
                }

            } catch (Exception e) {
                log.error("변경 사항 체크 실패: key={}", key, e);
            }
        });

        lastCheckTime = currentCheckTime;
    }

    private void sendSnapshotEvent(SseEmitter emitter, List<Allocation> allocations) throws IOException {
        AllocationStatusSnapShot snapshot = AllocationStatusSnapShot.from(allocations);
        String json = objectMapper.writeValueAsString(SuccessResponse.of(snapshot));
        emitter.send(SseEmitter.event().name("snapshot").data(json));
    }

    private void broadcastChanges(List<SseEmitter> emitterList, List<Allocation> changes) {
        try {
            AllocationStatusChanges changesEvent = AllocationStatusChanges.from(changes);
            String json = objectMapper.writeValueAsString(SuccessResponse.of(changesEvent));

            emitterList.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name("changes").data(json));
                } catch (IOException e) {
                    log.error("변경 사항 전송 실패", e);
                    emitterList.remove(emitter);
                }
            });
        } catch (Exception e) {
            log.error("변경 사항 브로드캐스트 실패", e);
        }
    }

    private String buildKey(Long matchId, Long blockId) {
        return matchId + ":" + blockId;
    }

    private Long[] parseKey(String key) {
        String[] parts = key.split(":");
        return new Long[] { Long.parseLong(parts[0]), Long.parseLong(parts[1]) };
    }
}
