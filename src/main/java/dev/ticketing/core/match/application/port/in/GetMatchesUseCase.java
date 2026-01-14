package dev.ticketing.core.match.application.port.in;

import dev.ticketing.core.match.application.port.in.model.GetMatchesQuery;
import dev.ticketing.core.match.application.port.in.model.MatchListResponse;

public interface GetMatchesUseCase {

    MatchListResponse getMatches(GetMatchesQuery query);
}
