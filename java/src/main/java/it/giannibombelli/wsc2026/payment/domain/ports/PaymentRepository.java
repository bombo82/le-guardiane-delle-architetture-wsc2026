package it.giannibombelli.wsc2026.payment.domain.ports;

import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    void save(Payment payment);

    Optional<Payment> findById(PaymentId id);

    Optional<Payment> findByClientReference(ClientReference clientReference);

    List<Payment> findAllRequestedAndProcessingBefore(Timestamp threshold);
}
