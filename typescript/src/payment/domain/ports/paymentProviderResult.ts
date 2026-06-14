// Risultato restituito da un PaymentProvider.

import { Description } from '@/common/domain/primitive/description.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type PaymentProviderResult =
  | { readonly kind: 'success'; readonly transactionId: Uuid; readonly providerCompletedAt: Timestamp }
  | { readonly kind: 'failure'; readonly reason: Description };

export function paymentProviderSuccess(transactionId: Uuid, providerCompletedAt: Timestamp): PaymentProviderResult {
  requireArgument(transactionId, 'transactionId');
  requireArgument(providerCompletedAt, 'providerCompletedAt');
  return { kind: 'success', transactionId, providerCompletedAt };
}

export function paymentProviderFailure(reason: Description): PaymentProviderResult {
  requireArgument(reason, 'reason');
  return { kind: 'failure', reason };
}
