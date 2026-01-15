package dev.ticketing.core.user.application.service.exception;

import org.springframework.http.HttpStatus;

import dev.ticketing.common.exception.DomainException;

public class UserException extends DomainException {

    public UserException(final String message, final HttpStatus status) {
        super(message, status);
    }

    public UserException(final String message, final HttpStatus status, final Throwable cause) {
        super(message, status, cause);
    }
}
