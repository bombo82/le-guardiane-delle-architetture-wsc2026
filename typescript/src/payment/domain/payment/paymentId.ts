// Identità dell'aggregato Payment.

import { requireArgument } from '@/common/utils/requireArgument.js';
import type { AggregateId } from '@/common/domain/identity/aggregateId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

export class PaymentId implements AggregateId {
  readonly value: Uuid;

  constructor(value: Uuid) {
    requireArgument(value, 'PaymentId value');
    this.value = value;
  }
}
