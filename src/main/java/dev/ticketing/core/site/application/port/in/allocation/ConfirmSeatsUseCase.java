package dev.ticketing.core.site.application.port.in.allocation;

import dev.ticketing.core.site.domain.allocation.Allocation;
import java.util.List;

/**
 * ConfirmSeatsUseCase - 좌석 선택 확정 처리 포트
 */
public interface ConfirmSeatsUseCase {
    /**
     * 좌석 선택 확정 (예약 확정 단계로 진입)
     */
    List<Allocation> confirmSeats(ConfirmSeatsCommand command);
}
