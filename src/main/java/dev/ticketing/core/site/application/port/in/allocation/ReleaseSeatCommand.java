package dev.ticketing.core.site.application.port.in.allocation;

import lombok.Builder;

/**
 * 좌석 반환(Release) Command
 */
@Builder
public record ReleaseSeatCommand(
        Long userId,
        Long matchId,
        Long seatId) {
}
