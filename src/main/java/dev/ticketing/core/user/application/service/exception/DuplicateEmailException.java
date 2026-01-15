package dev.ticketing.core.user.application.service.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends UserException {

    public DuplicateEmailException(final String email) {
        super("Email already exists: " + email, HttpStatus.CONFLICT);
    }
}
