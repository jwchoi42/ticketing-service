package dev.ticketing.core.site.application.port.in.allocation;

import java.util.List;

/**
 * 좌석 선택 확정 Command
 */
public record ConfirmSeatsCommand(
        Long userId,
        Long matchId,
        List<Long> seatIds) {
}
