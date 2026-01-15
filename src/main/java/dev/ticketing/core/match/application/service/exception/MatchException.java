package dev.ticketing.core.match.application.service.exception;

import org.springframework.http.HttpStatus;

import dev.ticketing.common.exception.DomainException;

public class MatchException extends DomainException {

    public MatchException(final String message, final HttpStatus status) {
        super(message, status);
    }

    public MatchException(final String message, final HttpStatus status, final Throwable cause) {
        super(message, status, cause);
    }
}
