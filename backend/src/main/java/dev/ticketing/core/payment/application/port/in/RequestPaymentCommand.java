package dev.ticketing.core.payment.application.port.in;

public record RequestPaymentCommand(
        Long reservationId,
        Integer amount,
        String method) {
}
