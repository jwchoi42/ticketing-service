package dev.ticketing.core.match.application.port.in.model;

import java.time.format.DateTimeFormatter;

import dev.ticketing.core.match.domain.Match;
import dev.ticketing.core.match.domain.MatchStatus;

public record MatchResponse(
        Long id,
        String stadium,
        String homeTeam,
        String awayTeam,
        String dateTime,
        MatchStatus status) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static MatchResponse from(final Match match) {
        return new MatchResponse(
                match.getId(),
                match.getStadium(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getDateTime().format(FORMATTER),
                match.getStatus());
    }
}
