package dev.ticketing.core.site.application.port.in.allocation.status;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SubscribeAllocationStatusChangesStreamUseCase - 실시간 좌석 현황 변경 스트림 구독 포트
 */
public interface SubscribeAllocationStatusChangesStreamUseCase {
    SseEmitter subscribeAllocationStatusChangesStream(Long matchId, Long blockId);
}
