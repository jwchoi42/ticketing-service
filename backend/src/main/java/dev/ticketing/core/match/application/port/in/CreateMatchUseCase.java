package dev.ticketing.core.match.application.port.in;

import dev.ticketing.core.match.application.port.in.model.CreateMatchCommand;
import dev.ticketing.core.match.application.port.in.model.MatchResponse;

public interface CreateMatchUseCase {

    MatchResponse createMatch(CreateMatchCommand command);
}
