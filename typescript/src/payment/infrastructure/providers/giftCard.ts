// Adapter per il provider GiftCard (stub che richiede un riferimento).

import { Description } from '@/common/domain/primitive/description.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { PaymentProvider } from '../../domain/ports/paymentProvider.js';
import {
  PaymentProviderResult,
  paymentProviderFailure,
  paymentProviderSuccess,
} from '../../domain/ports/paymentProviderResult.js';

export class GiftCard implements PaymentProvider {
  process(_paymentId: Uuid, providerReference: Uuid, _amount: Money): PaymentProviderResult {
    if (providerReference === null) {
      return paymentProviderFailure(new Description('gift card reference required'));
    }

    return paymentProviderSuccess(Uuid.generate(), Timestamp.now());
  }

  refund(_paymentId: Uuid, providerReference: Uuid, _amount: Money): PaymentProviderResult {
    if (providerReference === null) {
      return paymentProviderFailure(new Description('gift card reference required'));
    }

    return paymentProviderSuccess(Uuid.generate(), Timestamp.now());
  }
}
