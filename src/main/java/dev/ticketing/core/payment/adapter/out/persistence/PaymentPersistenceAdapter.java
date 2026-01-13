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
    public Optional<Payment> loadById(Long paymentId) {
        return paymentRepository.findById(paymentId).map(PaymentEntity::toDomain);
    }

    @Override
    public Optional<Payment> loadByReservationId(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId).map(PaymentEntity::toDomain);
    }

    @Override
    public Payment record(Payment payment) {
        return paymentRepository.save(PaymentEntity.from(payment)).toDomain();
    }
}
