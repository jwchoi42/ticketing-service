package dev.ticketing.core.match.application.port.out.persistence;

import dev.ticketing.core.match.domain.Match;

public interface RecordMatchPort {
    Match record(Match match);
}
