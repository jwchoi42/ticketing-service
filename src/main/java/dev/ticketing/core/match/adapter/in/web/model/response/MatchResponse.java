package dev.ticketing.core.match.adapter.in.web.model.response;

import dev.ticketing.core.match.domain.Match;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

@Schema(description = "경기 정보")
public record MatchResponse(
        @Schema(description = "경기 ID", example = "1") Long id,
        @Schema(description = "경기장", example = "Jamsil Baseball Stadium") String stadium,
        @Schema(description = "홈 팀", example = "LG Twins") String homeTeam,
        @Schema(description = "원정 팀", example = "Doosan Bears") String awayTeam,
        @Schema(description = "경기 일시", example = "2026-05-01 18:30:00") String dateTime
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static MatchResponse from(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getStadium(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getDateTime().format(FORMATTER)
        );
    }
}
