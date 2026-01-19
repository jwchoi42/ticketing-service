package dev.ticketing.core.match.application.port.out.persistence;

import dev.ticketing.core.match.domain.Match;

import java.util.List;
import java.util.Optional;

public interface LoadMatchPort {
    Optional<Match> loadById(Long matchId);
    List<Match> loadAll();
}
