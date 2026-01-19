package dev.ticketing.core.site.application.service.exception;

import org.springframework.http.HttpStatus;

import dev.ticketing.common.exception.DomainException;

public class SiteException extends DomainException {

    public SiteException(final String message, final HttpStatus status) {
        super(message, status);
    }

    public SiteException(final String message, final HttpStatus status, final Throwable cause) {
        super(message, status, cause);
    }
}
