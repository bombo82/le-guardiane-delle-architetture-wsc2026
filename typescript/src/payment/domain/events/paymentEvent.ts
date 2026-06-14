// Evento di dominio base per il Payment Bounded Context.

import type { PaymentRequested } from './paymentRequested.js';
import type { RefundRequested } from './refundRequested.js';
import type { PaymentDeadlineReached } from './paymentDeadlineReached.js';
import type { TransactionStarted } from './transactionStarted.js';
import type { TransactionAccepted } from './transactionAccepted.js';
import type { TransactionRejected } from './transactionRejected.js';
import type { PaymentResultEvent } from './paymentResultEvents.js';
import type { RefundResultEvent } from './refundResultEvents.js';

export type PaymentEvent =
  | PaymentRequested
  | RefundRequested
  | PaymentDeadlineReached
  | TransactionStarted
  | TransactionAccepted
  | TransactionRejected
  | PaymentResultEvent
  | RefundResultEvent;
