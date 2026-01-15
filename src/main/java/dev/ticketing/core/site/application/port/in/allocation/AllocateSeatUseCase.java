package dev.ticketing.core.site.application.port.in.allocation;

/**
 * AllocateSeatUseCase - 좌석 점유 처리 포트
 */
public interface AllocateSeatUseCase {
    /**
     * 좌석 점유 (Hold)
     */
    void allocateSeat(AllocateSeatCommand command);
}
