package dev.ticketing.core.site.application.port.in.allocation;

/**
 * ReleaseSeatUseCase - 좌석 점유 해제 처리 포트
 */
public interface ReleaseSeatUseCase {
    /**
     * 좌석 점유 해제 (Release)
     */
    void releaseSeat(ReleaseSeatCommand command);
}
