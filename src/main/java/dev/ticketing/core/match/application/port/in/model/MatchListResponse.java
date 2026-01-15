package dev.ticketing.core.match.application.port.in.model;

import java.util.List;

import dev.ticketing.core.match.domain.Match;

public record MatchListResponse(List<MatchResponse> matches) {

    public static MatchListResponse from(final List<Match> matches) {
        List<MatchResponse> matchResponses = matches.stream()
                .map(MatchResponse::from)
                .toList();
        return new MatchListResponse(matchResponses);
    }
}
