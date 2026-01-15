package dev.ticketing.core.payment.adapter.out.persistence;

import dev.ticketing.core.payment.application.port.out.persistence.LoadPaymentPort;
import dev.ticketing.core.payment.application.port.out.persistence.RecordPaymentPort;
import dev.ticketing.core.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements LoadPaymentPort, RecordPaymentPort {

    private final PaymentRepository paymentRepository;

    @Override
    public Optional<Payment> loadById(final Long paymentId) {
        return paymentRepository.findById(paymentId).map(PaymentEntity::toDomain);
    }

    @Override
    public Optional<Payment> loadByReservationId(final Long reservationId) {
        return paymentRepository.findByReservationId(reservationId).map(PaymentEntity::toDomain);
    }

    @Override
    public Payment record(final Payment payment) {
        return paymentRepository.save(PaymentEntity.from(payment)).toDomain();
    }
}
