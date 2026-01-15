package dev.ticketing.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;

    protected DomainException(final String message, final HttpStatus status) {
        super(message);
        this.status = status;
    }

    protected DomainException(final String message, final HttpStatus status, final Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
