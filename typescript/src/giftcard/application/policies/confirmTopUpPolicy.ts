// Policy che reagisce agli esiti del pagamento e richiede la conferma della ricarica.
// Violazione cross-BC didattica: GiftCard non dovrebbe importare tipi da Payment.

import type { Policy } from '@/common/application/policy.js';
import type { PaymentResultEvent } from '@/payment/domain/events/paymentResultEvents.js';
import { match } from 'ts-pattern';
import { ConfirmTopUp, confirmTopUp } from '../../application/commands/confirmTopUp.js';
import { GiftCardId } from '../../domain/giftcard/giftCardId.js';

export class ConfirmTopUpPolicy implements Policy<PaymentResultEvent, ConfirmTopUp> {
  evaluate(event: PaymentResultEvent): ConfirmTopUp | null {
    return match(event)
      .with(
        { kind: 'PaymentAccepted' },
        (accepted) => confirmTopUp(new GiftCardId(accepted.clientReference.value), accepted.amount)
      )
      .with({ kind: 'PaymentRejected' }, () => null)
      .with({ kind: 'PaymentExpired' }, () => null)
      .exhaustive();
  }
}
