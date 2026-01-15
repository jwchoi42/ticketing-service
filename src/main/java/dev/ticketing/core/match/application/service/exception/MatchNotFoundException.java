package dev.ticketing.core.match.application.service.exception;

import org.springframework.http.HttpStatus;

public class MatchNotFoundException extends MatchException {

    public MatchNotFoundException(final Long matchId) {
        super("Match not found: " + matchId, HttpStatus.NOT_FOUND);
    }
}
