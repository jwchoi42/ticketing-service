package dev.ticketing.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends DomainException {

    public UnauthorizedException(final String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
