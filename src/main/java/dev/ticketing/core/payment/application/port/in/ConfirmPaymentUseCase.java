package dev.ticketing.core.payment.application.port.in;

import dev.ticketing.core.payment.domain.Payment;

public interface ConfirmPaymentUseCase {
    Payment confirmPayment(ConfirmPaymentCommand command);
}
