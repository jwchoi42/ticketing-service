package dev.ticketing.core.match.application.service;

import dev.ticketing.core.match.application.port.in.GetMatchesUseCase;
import dev.ticketing.core.match.application.port.in.model.GetMatchesQuery;
import dev.ticketing.core.match.application.port.out.persistence.LoadMatchPort;
import dev.ticketing.core.match.domain.Match;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService implements GetMatchesUseCase {

    private final LoadMatchPort loadMatchPort;

    @Override
    public List<Match> getMatches(GetMatchesQuery query) {
        return loadMatchPort.loadAll();
    }
}
