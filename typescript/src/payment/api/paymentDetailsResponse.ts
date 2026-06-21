// DTO di risposta per un Payment con il dettaglio delle sue transazioni.

import { PaymentDetails } from '../application/query/paymentDetails.js';
import { toTransactionResponse, type TransactionResponse } from './transactionResponse.js';

export type PaymentDetailsResponse = {
  readonly id: string;
  readonly clientReference: string;
  readonly amount: number;
  readonly status: string;
  readonly requestedAt: Date;
  readonly transactions: TransactionResponse[];
};

export function toPaymentDetailsResponse(details: PaymentDetails): PaymentDetailsResponse {
  return {
    id: details.id,
    clientReference: details.clientReference.toString(),
    amount: details.amount.value,
    status: details.status,
    requestedAt: details.requestedAt.value,
    transactions: details.transactions.map((t) => toTransactionResponse(t)),
  };
}
