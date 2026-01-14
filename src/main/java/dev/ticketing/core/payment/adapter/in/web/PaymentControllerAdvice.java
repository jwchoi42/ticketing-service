package dev.ticketing.core.payment.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.ticketing.common.web.model.response.ErrorResponse;
import dev.ticketing.core.payment.application.service.exception.PaymentException;

@RestControllerAdvice(basePackages = "dev.ticketing.core.payment")
public class PaymentControllerAdvice {

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(final PaymentException e) {
        return ResponseEntity.status(e.getStatus()).body(ErrorResponse.of(e.getMessage()));
    }
}
