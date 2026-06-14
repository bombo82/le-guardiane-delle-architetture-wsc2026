// Adapter per il provider Klarna (stub di successo).

import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { PaymentProvider } from '../../domain/ports/paymentProvider.js';
import { PaymentProviderResult, paymentProviderSuccess } from '../../domain/ports/paymentProviderResult.js';

export class Klarna implements PaymentProvider {
  process(_paymentId: Uuid, _providerReference: Uuid, _amount: Money): PaymentProviderResult {
    return paymentProviderSuccess(Uuid.generate(), Timestamp.now());
  }

  refund(_paymentId: Uuid, _providerReference: Uuid, _amount: Money): PaymentProviderResult {
    return paymentProviderSuccess(Uuid.generate(), Timestamp.now());
  }
}
