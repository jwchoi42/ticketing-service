package dev.ticketing.core.match.application.port.in.model;

import java.time.LocalDateTime;

public record UpdateMatchCommand(
        Long matchId,
        String stadium,
        String homeTeam,
        String awayTeam,
        LocalDateTime dateTime) {
}
