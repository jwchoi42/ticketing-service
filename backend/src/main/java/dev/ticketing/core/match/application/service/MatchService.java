package dev.ticketing.core.match.application.service;

import java.util.List;

import dev.ticketing.core.match.application.port.in.GetMatchesUseCase;
import dev.ticketing.core.match.application.port.in.model.GetMatchesQuery;
import dev.ticketing.core.match.application.port.in.model.MatchListResponse;
import dev.ticketing.core.match.application.port.out.persistence.LoadMatchPort;
import dev.ticketing.core.match.domain.Match;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import dev.ticketing.core.match.application.port.in.GetMatchUseCase;
import dev.ticketing.core.match.application.port.in.model.MatchResponse;

import dev.ticketing.core.match.application.service.exception.MatchNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService implements GetMatchesUseCase, GetMatchUseCase {

    private final LoadMatchPort loadMatchPort;

    @Override
    public MatchListResponse getMatches(final GetMatchesQuery query) {
        List<Match> matches = loadMatchPort.loadAll();
        return MatchListResponse.from(matches);
    }

    @Override
    public MatchResponse getMatch(final Long matchId) {
        Match match = loadMatchPort.loadById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
        return MatchResponse.from(match);
    }
}
