// Read model dettagliato di un Payment con le sue transazioni.

import type { ClientReference } from '@/common/domain/primitive/clientReference.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { Timestamp } from '@/common/domain/primitive/timestamp.js';
import type { PaymentStatus } from '../../domain/payment/paymentStatus.js';
import type { Provider } from '../../domain/payment/provider.js';
import type { TransactionStatus } from '../../domain/payment/transactionStatus.js';

export type PaymentDetails = {
  readonly id: string;
  readonly clientReference: ClientReference;
  readonly amount: Money;
  readonly status: PaymentStatus;
  readonly requestedAt: Timestamp;
  readonly transactions: PaymentDetails.TransactionDetail[];
};

export namespace PaymentDetails {
  export type TransactionDetail = {
    readonly id: string;
    readonly provider: Provider;
    readonly providerReference: string | null;
    readonly amount: Money;
    readonly status: TransactionStatus;
    readonly startedAt: Timestamp;
    readonly completedAt: Timestamp | null;
  };
}
