package dev.ticketing.core.payment.application.port.in;

public record ConfirmPaymentCommand(
        Long paymentId,
        String paymentKey,
        String orderId,
        Integer amount) {
}
