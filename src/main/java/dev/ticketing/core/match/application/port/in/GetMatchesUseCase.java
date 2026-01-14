package dev.ticketing.core.match.application.port.in;

import dev.ticketing.core.match.application.port.in.model.GetMatchesQuery;
import dev.ticketing.core.match.domain.Match;

import java.util.List;

public interface GetMatchesUseCase {
    List<Match> getMatches(GetMatchesQuery query);
}
