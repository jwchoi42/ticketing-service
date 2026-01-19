package dev.ticketing.core.reservation.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.ticketing.common.web.model.response.ErrorResponse;
import dev.ticketing.core.reservation.application.service.exception.ReservationException;

@RestControllerAdvice(basePackages = "dev.ticketing.core.reservation")
public class ReservationControllerAdvice {

    @ExceptionHandler(ReservationException.class)
    public ResponseEntity<ErrorResponse> handleReservationException(final ReservationException e) {
        return ResponseEntity.status(e.getStatus()).body(ErrorResponse.of(e.getMessage()));
    }
}
