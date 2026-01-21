package dev.ticketing.core.match.application.port.in;

import dev.ticketing.core.match.application.port.in.model.MatchResponse;
import dev.ticketing.core.match.application.port.in.model.UpdateMatchCommand;

public interface UpdateMatchUseCase {

    MatchResponse updateMatch(UpdateMatchCommand command);
}
