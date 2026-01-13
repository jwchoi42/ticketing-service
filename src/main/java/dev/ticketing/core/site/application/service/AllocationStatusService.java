package dev.ticketing.core.site.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ticketing.common.web.model.response.SuccessResponse;
import dev.ticketing.core.site.adapter.in.web.status.AllocationStatusChanges;
import dev.ticketing.core.site.adapter.in.web.status.AllocationStatusSnapShot;
import dev.ticketing.core.site.application.port.in.allocation.status.GetAllocationStatusSnapShotUseCase;
import dev.ticketing.core.site.application.port.in.allocation.status.SubscribeAllocationStatusChangesStreamUseCase;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationStatusPort;
import dev.ticketing.core.site.domain.allocation.Allocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationStatusService
        implements GetAllocationStatusSnapShotUseCase, SubscribeAllocationStatusChangesStreamUseCase {

    private final LoadAllocationStatusPort loadAllocationStatusPort;
    private final ObjectMapper objectMapper;

    // matchId:blockId - stream
    private final Map<String, List<SseEmitter>> allocationStatusEmitters = new ConcurrentHashMap<>();

    // 마지막 확인 시간 (충분히 과거 시간으로 초기화하여 첫 번째 변경사항도 감지)
    private LocalDateTime lastCheckTime = LocalDateTime.now().minusMinutes(10);

    // UseCase 1: SnapShot 조회 (HTTP)
    @Override
    @Transactional(readOnly = true)
    public AllocationStatusSnapShot getAllocationStatusSnapShot(Long matchId, Long blockId) {
        List<Allocation> seats = loadAllocationStatusPort.loadAllocationStatusesByBlockId(matchId, blockId);
        return AllocationStatusSnapShot.from(seats);
    }

    // UseCase 2: Stream 구독 (SSE)
    @Override
    public SseEmitter subscribeAllocationStatusChangesStream(Long matchId, Long blockId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 무제한 타임아웃
        String key = matchId + ":" + blockId;

        // 1. SSE 연결 등록
        allocationStatusEmitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("SSE 연결 등록: matchId={}, blockId={}, 총 연결 수={}",
                matchId, blockId, allocationStatusEmitters.get(key).size());

        try {
            // 2. 초기 데이터 전송 (자신의 메서드 호출)
            AllocationStatusSnapShot snapshot = getAllocationStatusSnapShot(matchId, blockId);
            sendInitEvent(emitter, snapshot);
            log.info("초기 데이터 전송 완료: matchId={}, blockId={}, 좌석 수={}",
                    matchId, blockId, snapshot.seats().size());

        } catch (IOException e) {
            log.error("초기 데이터 전송 실패", e);
            allocationStatusEmitters.get(key).remove(emitter);
            emitter.completeWithError(e);
            return emitter;
        }

        // 3. 연결 종료 시 제거
        emitter.onCompletion(() -> {
            allocationStatusEmitters.get(key).remove(emitter);
            log.info("SSE 연결 종료: matchId={}, blockId={}, 남은 연결 수={}",
                    matchId, blockId, allocationStatusEmitters.get(key).size());
        });

        emitter.onTimeout(() -> {
            allocationStatusEmitters.get(key).remove(emitter);
            log.warn("SSE 연결 타임아웃: matchId={}, blockId={}", matchId, blockId);
        });

        emitter.onError(e -> {
            allocationStatusEmitters.get(key).remove(emitter);
            log.error("SSE 연결 에러: matchId={}, blockId={}", matchId, blockId, e);
        });

        return emitter;
    }

    /**
     * 1초마다 실행: 변경 사항 감지 및 SSE 전송
     */
    @Scheduled(fixedRate = 1000)
    public void checkForUpdates() {
        System.out.println("[DEBUG] 스케줄러 실행됨: emitters 수=" + allocationStatusEmitters.size());
        log.info("스케줄러 실행됨: emitters 수={}", allocationStatusEmitters.size());
        if (allocationStatusEmitters.isEmpty()) {
            System.out.println("[DEBUG] 연결된 클라이언트 없음 - 스킵");
            log.info("연결된 클라이언트 없음 - 스킵");
            return; // 연결된 클라이언트가 없으면 스킵
        }

        LocalDateTime currentCheckTime = LocalDateTime.now();

        log.info("변경 사항 체크 시작: 연결된 구간 수={}, 마지막 확인 시간={}, 현재 시간={}",
                allocationStatusEmitters.size(), lastCheckTime, currentCheckTime);

        allocationStatusEmitters.forEach((key, emitterList) -> {
            if (emitterList.isEmpty()) {
                return;
            }

            try {
                String[] parts = key.split(":");
                Long matchId = Long.parseLong(parts[0]);
                Long blockId = Long.parseLong(parts[1]);

                // DB에서 변경 사항 조회
                System.out.println("[DEBUG] 변경 사항 조회 시작: matchId=" + matchId + ", blockId=" + blockId + ", since="
                        + lastCheckTime);
                log.debug("변경 사항 조회 시작: matchId={}, blockId={}, since={}", matchId, blockId, lastCheckTime);
                List<Allocation> changes = loadAllocationStatusPort
                        .loadAllocationStatusesSince(matchId, blockId, lastCheckTime);
                System.out.println("[DEBUG] 변경 사항 조회 완료: matchId=" + matchId + ", blockId=" + blockId + ", 변경 수="
                        + changes.size());
                log.debug("변경 사항 조회 완료: matchId={}, blockId={}, 변경 수={}", matchId, blockId, changes.size());

                if (!changes.isEmpty()) {
                    System.out.println("[DEBUG] 변경 사항 감지!!! matchId=" + matchId + ", blockId=" + blockId + ", 변경 수="
                            + changes.size());
                    log.info("변경 사항 감지: matchId={}, blockId={}, 변경 수={}, 변경 목록={}",
                            matchId, blockId, changes.size(), changes);

                    // 변경분을 한 번의 이벤트로 전송 (Batch)
                    AllocationStatusChanges changesEvent = AllocationStatusChanges.from(changes);
                    String json = objectMapper.writeValueAsString(SuccessResponse.of(changesEvent));

                    // 모든 연결된 클라이언트에게 전송
                    emitterList.forEach(emitter -> {
                        try {
                            emitter.send(SseEmitter.event().name("changes").data(json));
                        } catch (IOException e) {
                            log.error("변경 사항 전송 실패", e);
                            emitterList.remove(emitter);
                        }
                    });
                }

            } catch (Exception e) {
                log.error("변경 사항 체크 실패: key={}", key, e);
            }
        });

        lastCheckTime = currentCheckTime;
    }

    private void sendInitEvent(SseEmitter emitter, AllocationStatusSnapShot snapshot) throws IOException {
        String json = objectMapper.writeValueAsString(SuccessResponse.of(snapshot));
        emitter.send(SseEmitter.event().name("snapshot").data(json));
    }
}
