package dev.ticketing.core.match.application.service.exception;

import org.springframework.http.HttpStatus;

public class MatchNotOpenException extends MatchException {

    public MatchNotOpenException(final Long matchId) {
        super("Match is not open for reservation: " + matchId, HttpStatus.BAD_REQUEST);
    }
}
