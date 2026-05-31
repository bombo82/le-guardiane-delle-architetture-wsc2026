package it.giannibombelli.wsc2026.payment.application.services;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.events.EventPublisher;
import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.payment.application.commands.RefundTransaction;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;
import it.giannibombelli.wsc2026.payment.domain.events.RefundRequested;
import it.giannibombelli.wsc2026.payment.domain.events.RefundResultEvents;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.payment.Transaction;
import it.giannibombelli.wsc2026.payment.domain.payment.TransactionStatus;
import it.giannibombelli.wsc2026.payment.domain.policies.TransactionRefund;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentProvider;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentProviderResult;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class RefundHandling implements EventSubscriber<RefundRequested> {
    private final PaymentRepository paymentRepository;
    private final Map<String, PaymentProvider> providers;
    private final TransactionRefund transactionRefund;
    private final EventPublisher<PaymentEvent> eventPublisher;

    public RefundHandling(PaymentRepository paymentRepository,
                          Map<String, PaymentProvider> providers,
                          TransactionRefund transactionRefund,
                          EventPublisher<PaymentEvent> eventPublisher) {
        this.paymentRepository = requireNonNull(paymentRepository);
        this.providers = requireNonNull(providers);
        this.transactionRefund = requireNonNull(transactionRefund);
        this.eventPublisher = requireNonNull(eventPublisher);
    }

    @Override
    public void on(RefundRequested event) {
        Require.requireArgument(event, "event");

        RefundTransaction cmd = transactionRefund.evaluate(event);

        Payment payment = paymentRepository.findById(cmd.aggregateId())
            .orElseThrow(() -> new IllegalArgumentException("payment not found"));

        List<Transaction> acceptedTransactions = payment.transactions().stream()
            .filter(t -> t.status() == TransactionStatus.ACCEPTED)
            .toList();

        if (acceptedTransactions.isEmpty()) {
            throw new IllegalStateException("payment has no accepted transaction");
        }

        Money refundedSum = Money.zero();
        Transaction failedTransaction = null;
        String failureReason = null;

        for (Transaction transaction : acceptedTransactions) {
            PaymentProvider provider = providers.get(transaction.provider().label());
            if (provider == null) {
                failedTransaction = transaction;
                failureReason = "unknown provider: " + transaction.provider().label();
                break;
            }

            PaymentProviderResult result = provider.refund(
                payment.id().value(),
                transaction.providerReference().value(),
                transaction.amount()
            );

            switch (result) {
                case PaymentProviderResult.Success s -> {
                    payment.markTransactionRefunded(transaction.id());
                    refundedSum = refundedSum.plus(transaction.amount());
                }
                case PaymentProviderResult.Failure f -> {
                    failedTransaction = transaction;
                    failureReason = f.reason().value();
                }
            }

            if (failedTransaction != null) {
                break;
            }
        }

        if (failedTransaction != null) {
            RefundResultEvents.TransactionNotRefunded notRefunded = payment.rejectRefund(
                failedTransaction.provider(), failedTransaction.providerReference(), new Description(failureReason)
            );
            paymentRepository.save(payment);
            eventPublisher.publish(notRefunded);
            return;
        }

        RefundTransaction refundedCmd = new RefundTransaction(payment.id(), refundedSum);
        RefundResultEvents.TransactionRefunded refunded = payment.refundTransaction(refundedCmd.amount());
        paymentRepository.save(payment);
        eventPublisher.publish(refunded);
    }
}
