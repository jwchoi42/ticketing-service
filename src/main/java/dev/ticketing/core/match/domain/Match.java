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

    public static Match create(String stadium, String homeTeam, String awayTeam, LocalDateTime dateTime) {
        return Match.builder()
                .stadium(stadium)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .dateTime(dateTime)
                .build();
    }

    public static Match withId(Long id, String stadium, String homeTeam, String awayTeam, LocalDateTime dateTime) {
        return Match.builder()
                .id(id)
                .stadium(stadium)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .dateTime(dateTime)
                .build();
    }
}
