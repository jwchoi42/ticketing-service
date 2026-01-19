package dev.ticketing.core.site.adapter.in.web.allocation.model.request;

import dev.ticketing.core.site.application.port.in.allocation.AllocateSeatCommand;

public record AllocateSeatRequest(Long userId) {
    public AllocateSeatCommand toCommand(Long matchId, Long seatId) {
        return AllocateSeatCommand.builder()
                .userId(userId)
                .matchId(matchId)
                .seatId(seatId)
                .build();
    }
}
