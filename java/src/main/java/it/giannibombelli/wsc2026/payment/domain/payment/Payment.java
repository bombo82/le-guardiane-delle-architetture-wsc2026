package it.giannibombelli.wsc2026.payment.domain.payment;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.model.Aggregate;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;
import it.giannibombelli.wsc2026.payment.domain.events.RefundRequested;
import it.giannibombelli.wsc2026.payment.domain.events.RefundResultEvents;
import it.giannibombelli.wsc2026.payment.domain.events.TransactionStarted;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Payment implements Aggregate<PaymentId> {
    private final PaymentId id;
    private final ClientReference clientReference;
    private final Money amount;
    private final Timestamp requestedAt;
    private PaymentStatus status;
    private final List<Transaction> transactions = new ArrayList<>();

    public Payment(PaymentId id, ClientReference clientReference, Money amount, PaymentStatus status, Timestamp requestedAt, List<Transaction> transactions) {
        Require.requireArgument(id, "id");
        Require.requireArgument(clientReference, "clientReference");
        Require.requireArgument(amount, "amount");
        Require.requireArgument(status, "status");
        Require.requireArgument(requestedAt, "requestedAt");
        Require.requireArgument(transactions, "transactions");
        this.id = id;
        this.clientReference = clientReference;
        this.amount = amount;
        this.status = status;
        this.requestedAt = requestedAt;
        this.transactions.addAll(transactions);
    }

    public PaymentId id() {
        return id;
    }

    public ClientReference clientReference() {
        return clientReference;
    }

    public Money amount() {
        return amount;
    }

    public PaymentStatus status() {
        return status;
    }

    public Timestamp requestedAt() {
        return requestedAt;
    }

    public List<Transaction> transactions() {
        return List.copyOf(transactions);
    }

    public static Payment request(PaymentId id, ClientReference clientReference, Money amount, Timestamp requestedAt) {
        Require.requireArgument(id, "id");
        Require.requireArgument(clientReference, "clientReference");
        Require.requireArgument(amount, "amount");
        Require.requireArgument(requestedAt, "requestedAt");

        return new Payment(id, clientReference, amount, PaymentStatus.REQUESTED, requestedAt, List.of());
    }

    public TransactionStarted startTransaction(TransactionId transactionId, Provider provider, ProviderReference providerReference, Money amount, Timestamp startedAt) {
        Require.requireArgument(transactionId, "transactionId");
        Require.requireArgument(provider, "provider");
        Require.requireArgument(amount, "amount");
        Require.requireArgument(startedAt, "startedAt");
        ensureMutable();

        Transaction transaction = Transaction.start(transactionId, provider, providerReference, amount, startedAt);
        transactions.add(transaction);

        this.status = PaymentStatus.PROCESSING;

        return new TransactionStarted(id, provider, transactionId, amount);
    }

    public PaymentResultEvents.PaymentAccepted acceptTransaction(TransactionId transactionId, Timestamp providerCompletedAt) {
        Require.requireArgument(transactionId, "transactionId");
        Require.requireArgument(providerCompletedAt, "providerCompletedAt");
        ensureMutable();

        Transaction transaction = findTransaction(transactionId);
        Transaction accepted = transaction.accept(providerCompletedAt);
        replaceTransaction(transaction, accepted);

        if (!isWithinAcceptanceWindow(providerCompletedAt)) {
            throw new IllegalStateException("transaction outside acceptance window");
        }

        if (allAcceptedAndTimely() && sufficientCoverage()) {
            this.status = PaymentStatus.ACCEPTED;
            return new PaymentResultEvents.PaymentAccepted(id, clientReference, amount);
        }

        return null;
    }

    public PaymentResultEvents.PaymentRejected rejectTransaction(TransactionId transactionId, Description reason) {
        Require.requireArgument(transactionId, "transactionId");
        Require.requireArgument(reason, "reason");
        ensureMutable();

        Transaction transaction = findTransaction(transactionId);
        Transaction rejected = transaction.reject(Timestamp.now());
        replaceTransaction(transaction, rejected);

        this.status = PaymentStatus.REJECTED;
        return new PaymentResultEvents.PaymentRejected(id, clientReference, amount, reason);
    }

    public PaymentResultEvents.PaymentExpired expire() {
        if (status == PaymentStatus.ACCEPTED) throw new IllegalStateException("payment already accepted");
        this.status = PaymentStatus.EXPIRED;
        return new PaymentResultEvents.PaymentExpired(id, clientReference, amount);
    }

    public RefundRequested requestRefund(Money amount) {
        Require.requireArgument(amount, "amount");
        if (this.status != PaymentStatus.ACCEPTED)
            throw new IllegalStateException("only accepted payments can be refunded");
        if (amount.isGreaterThan(this.amount)) {
            throw new IllegalArgumentException("refund amount cannot exceed payment amount");
        }
        return new RefundRequested(id, clientReference, amount);
    }

    public void markTransactionRefunded(TransactionId transactionId) {
        Require.requireArgument(transactionId, "transactionId");
        if (this.status != PaymentStatus.ACCEPTED)
            throw new IllegalStateException("only accepted payments can be refunded");
        Transaction transaction = findTransaction(transactionId);
        Transaction refunded = transaction.refund(Timestamp.now());
        replaceTransaction(transaction, refunded);
    }

    public RefundResultEvents.TransactionRefunded refundTransaction(Money amount) {
        Require.requireArgument(amount, "amount");
        if (this.status != PaymentStatus.ACCEPTED)
            throw new IllegalStateException("only accepted payments can be refunded");
        transactions.stream()
            .filter(t -> t.status() == TransactionStatus.ACCEPTED)
            .forEach(t -> markTransactionRefunded(t.id()));
        this.status = PaymentStatus.REFUNDED;
        return new RefundResultEvents.TransactionRefunded(id, clientReference, amount);
    }

    public RefundResultEvents.TransactionNotRefunded rejectRefund(Provider provider, ProviderReference providerReference, Description reason) {
        Require.requireArgument(provider, "provider");
        Require.requireArgument(providerReference, "providerReference");
        Require.requireArgument(reason, "reason");
        if (this.status != PaymentStatus.ACCEPTED)
            throw new IllegalStateException("only accepted payments can be refunded");
        return new RefundResultEvents.TransactionNotRefunded(id, clientReference, reason);
    }

    private void ensureMutable() {
        if (status == PaymentStatus.ACCEPTED) throw new IllegalStateException("payment already accepted");
        if (status == PaymentStatus.EXPIRED) throw new IllegalStateException("payment expired");
        if (status == PaymentStatus.REFUNDED) throw new IllegalStateException("payment refunded");
    }

    private Transaction findTransaction(TransactionId transactionId) {
        return transactions.stream()
            .filter(t -> t.id().equals(transactionId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("transaction not found"));
    }

    private void replaceTransaction(Transaction oldTransaction, Transaction newTransaction) {
        int index = transactions.indexOf(oldTransaction);
        if (index < 0) throw new IllegalStateException("transaction not found");
        transactions.set(index, newTransaction);
    }

    private boolean isWithinAcceptanceWindow(Timestamp providerCompletedAt) {
        if (requestedAt == null) return false;
        Timestamp deadline = requestedAt.plusSeconds(48 * 3600);
        return providerCompletedAt.isAfterOrEqual(requestedAt) && providerCompletedAt.isBeforeOrEqual(deadline);
    }

    private boolean allAcceptedAndTimely() {
        if (requestedAt == null) return false;
        if (transactions.isEmpty()) return false;
        boolean allAccepted = transactions.stream().allMatch(t -> t.status() == TransactionStatus.ACCEPTED);
        boolean allTimely = transactions.stream().allMatch(t -> isWithinAcceptanceWindow(t.completedAt()));
        return allAccepted && allTimely && sufficientCoverage();
    }

    private boolean sufficientCoverage() {
        Money sum = transactions.stream()
            .filter(t -> t.status() == TransactionStatus.ACCEPTED)
            .map(Transaction::amount)
            .reduce(Money.zero(), Money::plus);
        return !sum.isLessThan(amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((Payment) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Payment[" +
            "id=" + id + ", " +
            "clientReference=" + clientReference + ", " +
            "amount=" + amount + ", " +
            "status=" + status +
            ']';
    }
}
