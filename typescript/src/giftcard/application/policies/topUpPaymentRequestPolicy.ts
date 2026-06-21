// Policy che traduce GiftCardTopUpRequested in RequestPayment per il Payment BC.
// Violazione cross-BC didattica: GiftCard non dovrebbe importare tipi da Payment.

import type { Policy } from '@/common/application/policy.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { RequestPayment, requestPayment } from '@/payment/application/commands/requestPayment.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import { GiftCardTopUpRequested } from '../../domain/events/giftCardTopUpRequested.js';

export class TopUpPaymentRequestPolicy implements Policy<GiftCardTopUpRequested, RequestPayment> {
  evaluate(event: GiftCardTopUpRequested): RequestPayment {
    requireArgument(event, 'Top-up request event');
    return requestPayment(
      generateId((value) => new PaymentId(value)),
      new ClientReference(event.aggregateId.value),
      event.requestedAmount,
      Timestamp.now()
    );
  }
}
