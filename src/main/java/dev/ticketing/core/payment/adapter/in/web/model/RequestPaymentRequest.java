package dev.ticketing.core.payment.adapter.in.web.model;

import dev.ticketing.core.payment.application.port.in.RequestPaymentCommand;

public record RequestPaymentRequest(
        Long reservationId,
        Integer amount,
        String method) {
    public RequestPaymentCommand toCommand() {
        return new RequestPaymentCommand(reservationId, amount, method);
    }
}
