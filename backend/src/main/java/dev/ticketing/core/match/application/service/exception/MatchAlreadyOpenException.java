package dev.ticketing.core.match.application.service.exception;

import org.springframework.http.HttpStatus;

public class MatchAlreadyOpenException extends MatchException {

    public MatchAlreadyOpenException(final Long matchId) {
        super("Match is already open: " + matchId, HttpStatus.CONFLICT);
    }
}
