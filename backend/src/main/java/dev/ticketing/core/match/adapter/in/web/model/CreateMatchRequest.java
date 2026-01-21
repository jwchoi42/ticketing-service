package dev.ticketing.core.match.adapter.in.web.model;

import java.time.LocalDateTime;

import dev.ticketing.core.match.application.port.in.model.CreateMatchCommand;

public record CreateMatchRequest(
        String stadium,
        String homeTeam,
        String awayTeam,
        LocalDateTime dateTime) {

    public CreateMatchCommand toCommand() {
        return new CreateMatchCommand(stadium, homeTeam, awayTeam, dateTime);
    }
}
