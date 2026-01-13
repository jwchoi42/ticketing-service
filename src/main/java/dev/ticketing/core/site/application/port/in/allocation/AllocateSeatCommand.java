package dev.ticketing.core.site.application.port.in.allocation;

import lombok.Builder;

/**
 * 좌석 점유 Command
 */
@Builder
public record AllocateSeatCommand(
        Long userId,
        Long matchId,
        Long seatId) {
}
