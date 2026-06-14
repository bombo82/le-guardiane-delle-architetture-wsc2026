// DTO di risposta per una singola transazione di pagamento.

import { PaymentDetails } from '../application/query/paymentDetails.js';

export type TransactionResponse = {
  readonly id: string;
  readonly provider: string;
  readonly providerReference: string | null;
  readonly amount: number;
  readonly status: string;
  readonly startedAt: Date;
  readonly completedAt: Date | null;
};

export function toTransactionResponse(detail: PaymentDetails.TransactionDetail): TransactionResponse {
  return {
    id: detail.id,
    provider: detail.provider,
    providerReference: detail.providerReference,
    amount: detail.amount.value,
    status: detail.status,
    startedAt: detail.startedAt.value,
    completedAt: detail.completedAt === null ? null : detail.completedAt.value,
  };
}
