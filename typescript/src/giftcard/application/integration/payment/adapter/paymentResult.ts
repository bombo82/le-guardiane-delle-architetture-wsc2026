// Anti-Corruption Layer che traduce la Published Language esposta da `payment`
// nel command interno del BC `giftcard` per confermare una ricarica.
//
// Isola il modello di `giftcard` da eventuali evoluzioni del modello interno di `payment`:
// l'unico contratto condiviso è `PaymentResultIntegrationEvent`.

import type {
  PaymentAcceptedIntegrationEvent,
  PaymentResultIntegrationEvent,
} from '@/payment/integration/paymentResultIntegrationEvent.js';
import type { ConfirmTopUp } from '@/giftcard/application/commands/confirmTopUp.js';
import { confirmTopUp } from '@/giftcard/application/commands/confirmTopUp.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export class PaymentResult {
  adapt(event: PaymentResultIntegrationEvent): ConfirmTopUp | null {
    requireArgument(event, 'Payment result integration event');

    switch (event.kind) {
      case 'PaymentAcceptedIntegrationEvent':
        return this.adaptAccepted(event);
      case 'PaymentRejectedIntegrationEvent':
      case 'PaymentExpiredIntegrationEvent':
        return null;
    }
  }

  adaptAccepted(event: PaymentAcceptedIntegrationEvent): ConfirmTopUp {
    requireArgument(event, 'Payment accepted integration event');
    return confirmTopUp(
      new GiftCardId(Uuid.fromString(event.clientReference)),
      event.amount
    );
  }
}
