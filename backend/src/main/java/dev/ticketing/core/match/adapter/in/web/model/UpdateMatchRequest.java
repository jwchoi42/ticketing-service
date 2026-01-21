package dev.ticketing.core.match.adapter.in.web.model;

import java.time.LocalDateTime;

import dev.ticketing.core.match.application.port.in.model.UpdateMatchCommand;

public record UpdateMatchRequest(
        String stadium,
        String homeTeam,
        String awayTeam,
        LocalDateTime dateTime) {

    public UpdateMatchCommand toCommand(Long matchId) {
        return new UpdateMatchCommand(matchId, stadium, homeTeam, awayTeam, dateTime);
    }
}
