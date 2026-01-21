package dev.ticketing.core.match.adapter.out.persistence;

import dev.ticketing.core.match.application.port.out.persistence.LoadMatchPort;
import dev.ticketing.core.match.application.port.out.persistence.RecordMatchPort;
import dev.ticketing.core.match.domain.Match;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MatchPersistenceAdapter implements LoadMatchPort, RecordMatchPort {

    private final MatchRepository matchRepository;

    @Override
    public Optional<Match> loadById(final Long matchId) {
        return matchRepository.findById(matchId).map(MatchEntity::toDomain);
    }

    @Override
    public Optional<Match> loadByIdWithLock(final Long matchId) {
        return matchRepository.findByIdWithLock(matchId).map(MatchEntity::toDomain);
    }

    @Override
    public List<Match> loadAll() {
        return matchRepository.findAll().stream()
                .map(MatchEntity::toDomain)
                .toList();
    }

    @Override
    public Match record(final Match match) {
        MatchEntity entity = MatchEntity.from(match);
        return matchRepository.save(entity).toDomain();
    }

    @Override
    public void delete(final Long matchId) {
        matchRepository.deleteById(matchId);
    }
}
