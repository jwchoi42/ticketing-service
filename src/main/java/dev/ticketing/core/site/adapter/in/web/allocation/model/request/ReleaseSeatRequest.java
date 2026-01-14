package dev.ticketing.core.site.adapter.in.web.allocation.model.request;

import dev.ticketing.core.site.application.port.in.allocation.ReleaseSeatCommand;

/**
 * 좌석 반환 요청
 */
public record ReleaseSeatRequest(
        Long userId) {
    public ReleaseSeatCommand toCommand(Long matchId, Long seatId) {
        return ReleaseSeatCommand.builder()
                .userId(userId)
                .matchId(matchId)
                .seatId(seatId)
                .build();
    }
}
