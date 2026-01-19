package dev.ticketing.core.payment.adapter.in.web.model;

import dev.ticketing.core.payment.application.port.in.ConfirmPaymentCommand;

public record ConfirmPaymentRequest(
        Long paymentId,
        String paymentKey,
        String orderId,
        Integer amount) {
    public ConfirmPaymentCommand toCommand() {
        return new ConfirmPaymentCommand(paymentId, paymentKey, orderId, amount);
    }
}
