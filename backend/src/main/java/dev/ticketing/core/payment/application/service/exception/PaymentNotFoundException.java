package dev.ticketing.core.payment.application.service.exception;

import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends PaymentException {

    public PaymentNotFoundException(final Long paymentId) {
        super(String.format("Payment not found: %d", paymentId), HttpStatus.NOT_FOUND);
    }
}
