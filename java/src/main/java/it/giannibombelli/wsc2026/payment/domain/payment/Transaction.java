package it.giannibombelli.wsc2026.payment.domain.payment;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.model.Entity;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;

import java.util.UUID;

public final class Transaction implements Entity<TransactionId> {
    private final TransactionId id;
    private final Provider provider;
    private final ProviderReference providerReference;
    private final Money amount;
    private final TransactionStatus status;
    private final Timestamp startedAt;
    private final Timestamp completedAt;

    public Transaction(TransactionId id, Provider provider, ProviderReference providerReference, Money amount,
                       TransactionStatus status, Timestamp startedAt, Timestamp completedAt) {
        Require.requireArgument(id, "id");
        Require.requireArgument(provider, "provider");
        Require.requireArgument(amount, "amount");
        Require.requireArgument(status, "status");
        Require.requireArgument(startedAt, "startedAt");
        this.id = id;
        this.provider = provider;
        this.providerReference = providerReference;
        this.amount = amount;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public static Transaction start(TransactionId id, Provider provider, ProviderReference providerReference, Money amount, Timestamp startedAt) {
        return new Transaction(id, provider, providerReference, amount, TransactionStatus.STARTED, startedAt, null);
    }

    public Transaction accept(Timestamp completedAt) {
        if (status != TransactionStatus.STARTED) throw new IllegalStateException("transaction not started");
        Require.requireArgument(completedAt, "completedAt");
        return new Transaction(id, provider, providerReference, amount, TransactionStatus.ACCEPTED, startedAt, completedAt);
    }

    public Transaction reject(Timestamp completedAt) {
        if (status != TransactionStatus.STARTED) throw new IllegalStateException("transaction not started");
        Require.requireArgument(completedAt, "completedAt");
        return new Transaction(id, provider, providerReference, amount, TransactionStatus.REJECTED, startedAt, completedAt);
    }

    public Transaction refund(Timestamp refundedAt) {
        if (status != TransactionStatus.ACCEPTED) throw new IllegalStateException("transaction not accepted");
        Require.requireArgument(refundedAt, "refundedAt");
        return new Transaction(id, provider, providerReference, amount, TransactionStatus.REFUNDED, startedAt, refundedAt);
    }

    public TransactionId id() {
        return id;
    }

    public Provider provider() {
        return provider;
    }

    public ProviderReference providerReference() {
        return providerReference;
    }

    public Money amount() {
        return amount;
    }

    public TransactionStatus status() {
        return status;
    }

    public Timestamp startedAt() {
        return startedAt;
    }

    public Timestamp completedAt() {
        return completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id.equals(((Transaction) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
