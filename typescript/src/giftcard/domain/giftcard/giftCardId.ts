// Identità dell'aggregato GiftCard.

import { requireArgument } from '@/common/utils/requireArgument.js';
import type { AggregateId } from '@/common/domain/identity/aggregateId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

export class GiftCardId implements AggregateId {
  readonly value: Uuid;

  constructor(value: Uuid) {
    requireArgument(value, 'GiftCardId value');
    this.value = value;
  }
}
