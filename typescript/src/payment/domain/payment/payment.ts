// Aggregato Payment: gestisce il ciclo di vita di una richiesta di pagamento,
// delle transazioni e dei rimborsi.

import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import type { Aggregate } from '@/common/domain/model/aggregate.js';
import {
  paymentAccepted,
  paymentRejected,
  paymentExpired,
  type PaymentAccepted,
  type PaymentRejected,
  type PaymentExpired,
} from '../events/paymentResultEvents.js';
import { refundRequested, type RefundRequested } from '../events/refundRequested.js';
import {
  transactionRefunded,
  transactionNotRefunded,
  type TransactionRefunded,
  type TransactionNotRefunded,
} from '../events/refundResultEvents.js';
import { transactionStarted, type TransactionStarted } from '../events/transactionStarted.js';
import { PaymentId } from './paymentId.js';
import { PaymentStatus } from './paymentStatus.js';
import { Provider } from './provider.js';
import { ProviderReference } from './providerReference.js';
import { Transaction } from './transaction.js';
import { TransactionId } from './transactionId.js';
import { TransactionNotFoundException } from './transactionNotFoundException.js';
import { TransactionStatus } from './transactionStatus.js';
import { requireArgument } from '@/common/utils/requireArgument.js';


export class Payment implements Aggregate<PaymentId> {
  private readonly _id: PaymentId;
  private readonly _clientReference: ClientReference;
  private readonly _amount: Money;
  private readonly _requestedAt: Timestamp;
  private _status: PaymentStatus;
  private readonly _transactions: Transaction[];

  constructor(
    id: PaymentId,
    clientReference: ClientReference,
    amount: Money,
    status: PaymentStatus,
    requestedAt: Timestamp,
    transactions: Transaction[]
  ) {
    requireArgument(id, 'id');
    requireArgument(clientReference, 'clientReference');
    requireArgument(amount, 'amount');
    requireArgument(status, 'status');
    requireArgument(requestedAt, 'requestedAt');
    requireArgument(transactions, 'transactions');

    this._id = id;
    this._clientReference = clientReference;
    this._amount = amount;
    this._status = status;
    this._requestedAt = requestedAt;
    this._transactions = [...transactions];
  }

  static request(id: PaymentId, clientReference: ClientReference, amount: Money, requestedAt: Timestamp): Payment {
    requireArgument(id, 'id');
    requireArgument(clientReference, 'clientReference');
    requireArgument(amount, 'amount');
    requireArgument(requestedAt, 'requestedAt');

    return new Payment(id, clientReference, amount, PaymentStatus.REQUESTED, requestedAt, []);
  }

  id(): PaymentId {
    return this._id;
  }

  clientReference(): ClientReference {
    return this._clientReference;
  }

  amount(): Money {
    return this._amount;
  }

  status(): PaymentStatus {
    return this._status;
  }

  requestedAt(): Timestamp {
    return this._requestedAt;
  }

  transactions(): Transaction[] {
    return [...this._transactions];
  }

  startTransaction(
    transactionId: TransactionId,
    provider: Provider,
    providerReference: ProviderReference | null,
    amount: Money,
    startedAt: Timestamp
  ): TransactionStarted {
    requireArgument(transactionId, 'transactionId');
    requireArgument(provider, 'provider');
    requireArgument(amount, 'amount');
    requireArgument(startedAt, 'startedAt');
    this.ensureMutable();

    const transaction = Transaction.start(transactionId, provider, providerReference, amount, startedAt);
    this._transactions.push(transaction);
    this._status = PaymentStatus.PROCESSING;

    return transactionStarted(this._id, provider, transactionId, amount);
  }

  acceptTransaction(transactionId: TransactionId, providerCompletedAt: Timestamp): PaymentAccepted | null {
    requireArgument(transactionId, 'transactionId');
    requireArgument(providerCompletedAt, 'providerCompletedAt');
    this.ensureMutable();

    const transaction = this.findTransaction(transactionId);
    const accepted = transaction.accept(providerCompletedAt);
    this.replaceTransaction(transaction, accepted);

    if (!this.isWithinAcceptanceWindow(providerCompletedAt)) {
      throw new Error('transaction outside acceptance window');
    }

    if (this.allAcceptedAndTimely()) {
      this._status = PaymentStatus.ACCEPTED;
      return paymentAccepted(this._id, this._clientReference, this._amount);
    }

    return null;
  }

  rejectTransaction(transactionId: TransactionId, reason: Description): PaymentRejected {
    requireArgument(transactionId, 'transactionId');
    requireArgument(reason, 'reason');
    this.ensureMutable();

    const transaction = this.findTransaction(transactionId);
    const rejected = transaction.reject(Timestamp.now());
    this.replaceTransaction(transaction, rejected);

    this._status = PaymentStatus.REJECTED;
    return paymentRejected(this._id, this._clientReference, this._amount, reason);
  }

  expire(): PaymentExpired {
    if (this._status === PaymentStatus.ACCEPTED) throw new Error('payment already accepted');
    this._status = PaymentStatus.EXPIRED;
    return paymentExpired(this._id, this._clientReference, this._amount);
  }

  requestRefund(amount: Money): RefundRequested {
    requireArgument(amount, 'amount');
    if (this._status !== PaymentStatus.ACCEPTED) throw new Error('only accepted payments can be refunded');
    if (amount.isGreaterThan(this._amount)) {
      throw new Error('refund amount cannot exceed payment amount');
    }
    return refundRequested(this._id, this._clientReference, amount);
  }

  markTransactionRefunded(transactionId: TransactionId): void {
    requireArgument(transactionId, 'transactionId');
    if (this._status !== PaymentStatus.ACCEPTED) throw new Error('only accepted payments can be refunded');
    const transaction = this.findTransaction(transactionId);
    const refunded = transaction.refund(Timestamp.now());
    this.replaceTransaction(transaction, refunded);
  }

  refundTransaction(amount: Money): TransactionRefunded {
    requireArgument(amount, 'amount');
    if (this._status !== PaymentStatus.ACCEPTED) throw new Error('only accepted payments can be refunded');
    this._transactions
      .filter((t) => t.status() === TransactionStatus.ACCEPTED)
      .forEach((t) => this.markTransactionRefunded(t.id()));
    this._status = PaymentStatus.REFUNDED;
    return transactionRefunded(this._id, this._clientReference, amount);
  }

  rejectRefund(provider: Provider, providerReference: ProviderReference, reason: Description): TransactionNotRefunded {
    requireArgument(provider, 'provider');
    requireArgument(providerReference, 'providerReference');
    requireArgument(reason, 'reason');
    if (this._status !== PaymentStatus.ACCEPTED) throw new Error('only accepted payments can be refunded');
    return transactionNotRefunded(this._id, this._clientReference, reason);
  }

  private ensureMutable(): void {
    if (this._status === PaymentStatus.ACCEPTED) throw new Error('payment already accepted');
    if (this._status === PaymentStatus.EXPIRED) throw new Error('payment expired');
    if (this._status === PaymentStatus.REFUNDED) throw new Error('payment refunded');
  }

  private findTransaction(transactionId: TransactionId): Transaction {
    const transaction = this._transactions.find((t) => t.id().equals(transactionId));
    if (transaction === undefined) throw new TransactionNotFoundException();
    return transaction;
  }

  private replaceTransaction(oldTransaction: Transaction, newTransaction: Transaction): void {
    const index = this._transactions.indexOf(oldTransaction);
    if (index < 0) throw new TransactionNotFoundException();
    this._transactions[index] = newTransaction;
  }

  private isWithinAcceptanceWindow(providerCompletedAt: Timestamp): boolean {
    const deadline = this._requestedAt.plusSeconds(48 * 3600);
    return providerCompletedAt.isAfterOrEqual(this._requestedAt) && providerCompletedAt.isBeforeOrEqual(deadline);
  }

  private allAcceptedAndTimely(): boolean {
    if (this._transactions.length === 0) return false;
    const allAccepted = this._transactions.every((t) => t.status() === TransactionStatus.ACCEPTED);
    const allTimely = this._transactions.every(
      (t) => t.completedAt() !== null && this.isWithinAcceptanceWindow(t.completedAt()!)
    );
    return allAccepted && allTimely && this.sufficientCoverage();
  }

  private sufficientCoverage(): boolean {
    const sum = this._transactions
      .filter((t) => t.status() === TransactionStatus.ACCEPTED)
      .reduce((acc, t) => acc.plus(t.amount()), Money.zero());
    return !sum.isLessThan(this._amount);
  }

  equals(other: Payment): boolean {
    return this._id.value.value === other._id.value.value;
  }

  hashCode(): number {
    return this._id.value.value.split('').reduce((hash, char) => {
      hash = (hash << 5) - hash + char.charCodeAt(0);
      return hash | 0;
    }, 0);
  }
}
