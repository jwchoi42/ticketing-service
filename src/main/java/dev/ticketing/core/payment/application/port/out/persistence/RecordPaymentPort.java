package dev.ticketing.core.payment.application.port.out.persistence;

import dev.ticketing.core.payment.domain.Payment;

public interface RecordPaymentPort {
    Payment record(Payment payment);
}
