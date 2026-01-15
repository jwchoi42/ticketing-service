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

    public static Match create(final String stadium, final String homeTeam, final String awayTeam,
            final LocalDateTime dateTime) {
        validate(stadium, homeTeam, awayTeam, dateTime);
        return Match.builder()
                .stadium(stadium)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .dateTime(dateTime)
                .build();
    }

    public static Match withId(final Long id, final String stadium, final String homeTeam, final String awayTeam,
            final LocalDateTime dateTime) {
        validate(stadium, homeTeam, awayTeam, dateTime);
        return Match.builder()
                .id(id)
                .stadium(stadium)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .dateTime(dateTime)
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
