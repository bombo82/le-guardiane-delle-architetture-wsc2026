// Policy che traduce un BookingPlaced in una richiesta di pagamento per il Payment BC.


import type { Policy } from '@/common/domain/model/policy.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { RequestPayment, requestPayment } from '@/payment/application/commands/requestPayment.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import { BookingPlaced } from '../events/bookingPlaced.js';

export class BookingPaymentRequestPolicy implements Policy<BookingPlaced, RequestPayment> {
  evaluate(event: BookingPlaced): RequestPayment {
    requireArgument(event, 'Booking placed event');
    return requestPayment(
      generateId((value) => new PaymentId(value)),
      new ClientReference(event.aggregateId.value),
      event.amount,
      Timestamp.now()
    );
  }
}
