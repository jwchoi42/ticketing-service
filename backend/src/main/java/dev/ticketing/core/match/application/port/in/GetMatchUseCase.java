package dev.ticketing.core.match.application.port.in;

import dev.ticketing.core.match.application.port.in.model.MatchResponse;

public interface GetMatchUseCase {

    MatchResponse getMatch(Long matchId);
}
