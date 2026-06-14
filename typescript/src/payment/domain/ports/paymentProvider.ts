// Porta per i provider di pagamento esterni.

import { Uuid } from '@/common/domain/primitive/uuid.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { PaymentProviderResult } from './paymentProviderResult.js';

export interface PaymentProvider {
  process(paymentId: Uuid, providerReference: Uuid, amount: Money): PaymentProviderResult;

  refund(paymentId: Uuid, providerReference: Uuid, amount: Money): PaymentProviderResult;
}
