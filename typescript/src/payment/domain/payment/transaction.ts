// Value Object/Entity Transaction all'interno dell'aggregato Payment.

import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { Provider } from './provider.js';
import { ProviderReference } from './providerReference.js';
import { TransactionId } from './transactionId.js';
import { TransactionStatus } from './transactionStatus.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export class Transaction {
  private readonly _id: TransactionId;
  private readonly _provider: Provider;
  private readonly _providerReference: ProviderReference | null;
  private readonly _amount: Money;
  private readonly _status: TransactionStatus;
  private readonly _startedAt: Timestamp;
  private readonly _completedAt: Timestamp | null;

  constructor(
    id: TransactionId,
    provider: Provider,
    providerReference: ProviderReference | null,
    amount: Money,
    status: TransactionStatus,
    startedAt: Timestamp,
    completedAt: Timestamp | null
  ) {
    requireArgument(id, 'id');
    requireArgument(provider, 'provider');
    requireArgument(amount, 'amount');
    requireArgument(status, 'status');
    requireArgument(startedAt, 'startedAt');

    this._id = id;
    this._provider = provider;
    this._providerReference = providerReference;
    this._amount = amount;
    this._status = status;
    this._startedAt = startedAt;
    this._completedAt = completedAt;
  }

  static start(
    id: TransactionId,
    provider: Provider,
    providerReference: ProviderReference | null,
    amount: Money,
    startedAt: Timestamp
  ): Transaction {
    return new Transaction(id, provider, providerReference, amount, TransactionStatus.STARTED, startedAt, null);
  }

  accept(completedAt: Timestamp): Transaction {
    if (this._status !== TransactionStatus.STARTED) throw new Error('transaction not started');
    requireArgument(completedAt, 'completedAt');
    return new Transaction(
      this._id,
      this._provider,
      this._providerReference,
      this._amount,
      TransactionStatus.ACCEPTED,
      this._startedAt,
      completedAt
    );
  }

  reject(completedAt: Timestamp): Transaction {
    if (this._status !== TransactionStatus.STARTED) throw new Error('transaction not started');
    requireArgument(completedAt, 'completedAt');
    return new Transaction(
      this._id,
      this._provider,
      this._providerReference,
      this._amount,
      TransactionStatus.REJECTED,
      this._startedAt,
      completedAt
    );
  }

  refund(refundedAt: Timestamp): Transaction {
    if (this._status !== TransactionStatus.ACCEPTED) throw new Error('transaction not accepted');
    requireArgument(refundedAt, 'refundedAt');
    return new Transaction(
      this._id,
      this._provider,
      this._providerReference,
      this._amount,
      TransactionStatus.REFUNDED,
      this._startedAt,
      refundedAt
    );
  }

  id(): TransactionId {
    return this._id;
  }

  provider(): Provider {
    return this._provider;
  }

  providerReference(): ProviderReference | null {
    return this._providerReference;
  }

  amount(): Money {
    return this._amount;
  }

  status(): TransactionStatus {
    return this._status;
  }

  startedAt(): Timestamp {
    return this._startedAt;
  }

  completedAt(): Timestamp | null {
    return this._completedAt;
  }

  equals(other: Transaction): boolean {
    return this._id.equals(other._id);
  }

  hashCode(): number {
    return this._id.hashCode();
  }
}
