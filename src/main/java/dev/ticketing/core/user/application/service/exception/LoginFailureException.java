package dev.ticketing.core.user.application.service.exception;

import org.springframework.http.HttpStatus;

public class LoginFailureException extends UserException {

    public LoginFailureException(final String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
