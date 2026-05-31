package it.giannibombelli.wsc2026.payment.application.query;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.Transaction;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentRepository;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class PaymentFinder {
    private final PaymentRepository paymentRepository;

    public PaymentFinder(PaymentRepository paymentRepository) {
        this.paymentRepository = requireNonNull(paymentRepository);
    }

    public Optional<PaymentDetails> findDetailsById(PaymentId id) {
        Require.requireArgument(id, "paymentId");

        return paymentRepository.findById(id).map(this::toDetails);
    }

    public Optional<PaymentSummary> findSummaryById(PaymentId id) {
        Require.requireArgument(id, "paymentId");

        return paymentRepository.findById(id).map(this::toSummary);
    }

    public Optional<PaymentDetails> findDetailsByClientReference(ClientReference clientReference) {
        Require.requireArgument(clientReference, "clientReference");

        return paymentRepository.findByClientReference(clientReference).map(this::toDetails);
    }

    private PaymentDetails toDetails(Payment payment) {
        return new PaymentDetails(
            payment.id().value(),
            payment.clientReference(),
            payment.amount(),
            payment.status(),
            payment.requestedAt(),
            payment.transactions().stream().map(this::toTransactionDetail).toList()
        );
    }

    private PaymentSummary toSummary(Payment payment) {
        return new PaymentSummary(
            payment.id().value(),
            payment.clientReference(),
            payment.amount(),
            payment.status(),
            payment.requestedAt()
        );
    }

    private PaymentDetails.TransactionDetail toTransactionDetail(Transaction transaction) {
        return new PaymentDetails.TransactionDetail(
            transaction.id().value(),
            transaction.provider(),
            transaction.providerReference() == null ? null : transaction.providerReference().value(),
            transaction.amount(),
            transaction.status(),
            transaction.startedAt(),
            transaction.completedAt()
        );
    }
}
