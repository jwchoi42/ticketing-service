package dev.ticketing.core.payment.application.service.exception;

import org.springframework.http.HttpStatus;

import dev.ticketing.common.exception.DomainException;

/**
 * Payment 도메인 최상위 예외
 */
public class PaymentException extends DomainException {

    public PaymentException(final String message, final HttpStatus status) {
        super(message, status);
    }
}
