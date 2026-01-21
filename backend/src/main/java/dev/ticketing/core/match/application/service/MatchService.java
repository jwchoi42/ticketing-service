package dev.ticketing.core.match.application.service;

import java.util.List;

import dev.ticketing.core.match.application.port.in.CreateMatchUseCase;
import dev.ticketing.core.match.application.port.in.DeleteMatchUseCase;
import dev.ticketing.core.match.application.port.in.GetMatchesUseCase;
import dev.ticketing.core.match.application.port.in.GetMatchUseCase;
import dev.ticketing.core.match.application.port.in.OpenMatchUseCase;
import dev.ticketing.core.match.application.port.in.UpdateMatchUseCase;
import dev.ticketing.core.match.application.port.in.model.CreateMatchCommand;
import dev.ticketing.core.match.application.port.in.model.GetMatchesQuery;
import dev.ticketing.core.match.application.port.in.model.MatchListResponse;
import dev.ticketing.core.match.application.port.in.model.MatchResponse;
import dev.ticketing.core.match.application.port.in.model.UpdateMatchCommand;
import dev.ticketing.core.match.application.port.out.persistence.LoadMatchPort;
import dev.ticketing.core.match.application.port.out.persistence.RecordMatchPort;
import dev.ticketing.core.match.application.service.exception.MatchAlreadyOpenException;
import dev.ticketing.core.match.application.service.exception.MatchNotFoundException;
import dev.ticketing.core.match.domain.Match;
import dev.ticketing.core.site.application.port.out.persistence.allocation.RecordAllocationPort;
import dev.ticketing.core.site.application.port.out.persistence.hierarchy.LoadSeatPort;
import dev.ticketing.core.site.domain.allocation.Allocation;
import dev.ticketing.core.site.domain.hierarchy.Seat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService implements GetMatchesUseCase, GetMatchUseCase,
        CreateMatchUseCase, UpdateMatchUseCase, DeleteMatchUseCase, OpenMatchUseCase {

    private final LoadMatchPort loadMatchPort;
    private final RecordMatchPort recordMatchPort;
    private final LoadSeatPort loadSeatPort;
    private final RecordAllocationPort recordAllocationPort;

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

    @Override
    @Transactional
    public MatchResponse createMatch(final CreateMatchCommand command) {
        Match match = Match.create(
                command.stadium(),
                command.homeTeam(),
                command.awayTeam(),
                command.dateTime()
        );
        Match savedMatch = recordMatchPort.record(match);
        return MatchResponse.from(savedMatch);
    }

    @Override
    @Transactional
    public MatchResponse updateMatch(final UpdateMatchCommand command) {
        Match match = loadMatchPort.loadById(command.matchId())
                .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

        Match updatedMatch = match.update(
                command.stadium(),
                command.homeTeam(),
                command.awayTeam(),
                command.dateTime()
        );
        Match savedMatch = recordMatchPort.record(updatedMatch);
        return MatchResponse.from(savedMatch);
    }

    @Override
    @Transactional
    public void deleteMatch(final Long matchId) {
        loadMatchPort.loadById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
        recordMatchPort.delete(matchId);
    }

    @Override
    @Transactional
    public MatchResponse openMatch(final Long matchId) {
        // Use pessimistic lock to prevent race condition when opening match
        Match match = loadMatchPort.loadByIdWithLock(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));

        if (match.isOpen()) {
            throw new MatchAlreadyOpenException(matchId);
        }

        Match openedMatch = match.open();
        Match savedMatch = recordMatchPort.record(openedMatch);

        prePopulateAllocations(savedMatch.getId());

        return MatchResponse.from(savedMatch);
    }

    private void prePopulateAllocations(final Long matchId) {
        List<Seat> allSeats = loadSeatPort.loadAllSeats();
        List<Allocation> allocations = allSeats.stream()
                .map(seat -> Allocation.availableForMatch(matchId, seat.getId()))
                .toList();
        recordAllocationPort.saveAll(allocations);
    }
}
