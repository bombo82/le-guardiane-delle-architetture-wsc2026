// Porta per la persistenza degli aggregati Payment.

import type { ClientReference } from '@/common/domain/primitive/clientReference.js';
import type { Timestamp } from '@/common/domain/primitive/timestamp.js';
import type { Payment } from '../payment/payment.js';
import type { PaymentId } from '../payment/paymentId.js';

export interface PaymentRepository {
  save(payment: Payment): void;

  findById(id: PaymentId): Payment | null;

  findByClientReference(clientReference: ClientReference): Payment | null;

  findAllRequestedAndProcessingBefore(threshold: Timestamp): Payment[];
}
