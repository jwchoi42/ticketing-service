package dev.ticketing.core.match.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Match {
    private Long id;
    private String stadium;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime dateTime;
    private MatchStatus status;

    public static Match create(final String stadium, final String homeTeam, final String awayTeam,
            final LocalDateTime dateTime) {
        validate(stadium, homeTeam, awayTeam, dateTime);
        return Match.builder()
                .stadium(stadium)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .dateTime(dateTime)
                .status(MatchStatus.DRAFT)
                .build();
    }

    public static Match withId(final Long id, final String stadium, final String homeTeam, final String awayTeam,
            final LocalDateTime dateTime, final MatchStatus status) {
        validate(stadium, homeTeam, awayTeam, dateTime);
        return Match.builder()
                .id(id)
                .stadium(stadium)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .dateTime(dateTime)
                .status(status)
                .build();
    }

    public boolean isOpen() {
        return this.status == MatchStatus.OPEN;
    }

    public Match open() {
        return Match.builder()
                .id(this.id)
                .stadium(this.stadium)
                .homeTeam(this.homeTeam)
                .awayTeam(this.awayTeam)
                .dateTime(this.dateTime)
                .status(MatchStatus.OPEN)
                .build();
    }

    public Match update(final String stadium, final String homeTeam, final String awayTeam, final LocalDateTime dateTime) {
        validate(stadium, homeTeam, awayTeam, dateTime);
        return Match.builder()
                .id(this.id)
                .stadium(stadium)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .dateTime(dateTime)
                .status(this.status)
                .build();
    }

    private static void validate(final String stadium, final String homeTeam, final String awayTeam,
            final LocalDateTime dateTime) {
        if (stadium == null || stadium.isBlank()) {
            throw new IllegalArgumentException("Stadium cannot be empty");
        }
        if (homeTeam == null || homeTeam.isBlank()) {
            throw new IllegalArgumentException("Home team cannot be empty");
        }
        if (awayTeam == null || awayTeam.isBlank()) {
            throw new IllegalArgumentException("Away team cannot be empty");
        }
        if (dateTime == null) {
            throw new IllegalArgumentException("Date time cannot be null");
        }
    }
}
