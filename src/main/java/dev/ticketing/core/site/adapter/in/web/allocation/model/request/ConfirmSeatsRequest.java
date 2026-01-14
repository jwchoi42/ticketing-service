package dev.ticketing.core.site.adapter.in.web.allocation.model.request;

import dev.ticketing.core.site.application.port.in.allocation.ConfirmSeatsCommand;

import java.util.List;

/**
 * 좌석 선택 확정 요청
 */
public record ConfirmSeatsRequest(
        Long userId,
        List<Long> seatIds) {
    public ConfirmSeatsCommand toCommand(Long matchId) {
        return new ConfirmSeatsCommand(userId, matchId, seatIds);
    }
}
