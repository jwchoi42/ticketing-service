package dev.ticketing.core.payment.application.service.exception;

import org.springframework.http.HttpStatus;

import dev.ticketing.core.payment.domain.PaymentStatus;

public class InvalidPaymentStateException extends PaymentException {

    public InvalidPaymentStateException(final Long paymentId, final PaymentStatus status) {
        super(String.format("Payment %d is in invalid state: %s", paymentId, status), HttpStatus.CONFLICT);
    }
}
