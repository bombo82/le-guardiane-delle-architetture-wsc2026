// Factory di aggregati per i test del Payment Bounded Context.

import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { Payment } from '@/payment/domain/payment/payment.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';

export class PaymentAggregateFactory {
  static createPayment(clientReference: string = crypto.randomUUID(), amount: Money = new Money(50)): Payment {
    const paymentId = generateId((value) => new PaymentId(value));
    return Payment.request(paymentId, new ClientReference(clientReference), amount, Timestamp.now());
  }
}
