package dev.ticketing.core.match.adapter.out.persistence;

import dev.ticketing.core.match.domain.Match;
import dev.ticketing.core.match.domain.MatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "matches")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stadium;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime dateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    public static MatchEntity from(final Match match) {
        return new MatchEntity(
                match.getId(),
                match.getStadium(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getDateTime(),
                match.getStatus());
    }

    public static MatchEntity fromId(final Long id) {
        MatchEntity entity = new MatchEntity();
        entity.id = id;
        return entity;
    }

    public Match toDomain() {
        return Match.withId(id, stadium, homeTeam, awayTeam, dateTime, status);
    }
}
