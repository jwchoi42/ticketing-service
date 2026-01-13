package dev.ticketing.core.match.adapter.in.web.model.response;

import dev.ticketing.core.match.domain.Match;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "경기 목록")
public record MatchListResponse(
        @Schema(description = "경기 목록") List<MatchResponse> matches
) {
    public static MatchListResponse from(List<Match> matches) {
        List<MatchResponse> matchResponses = matches.stream()
                .map(MatchResponse::from)
                .toList();
        return new MatchListResponse(matchResponses);
    }
}
