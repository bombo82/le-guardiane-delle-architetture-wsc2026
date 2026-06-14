// Identità di una Transaction all'interno di un Payment.

import { requireArgument } from '@/common/utils/requireArgument.js';
import type { EntityId } from '@/common/domain/identity/entityId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

export class TransactionId implements EntityId {
  readonly value: Uuid;

  constructor(value: Uuid) {
    requireArgument(value, 'TransactionId value');
    this.value = value;
  }

  equals(other: TransactionId): boolean {
    return this.value.value === other.value.value;
  }

  hashCode(): number {
    let hash = 0;
    const value = this.value.value;
    for (let i = 0; i < value.length; i++) {
      hash = (hash << 5) - hash + value.charCodeAt(i);
      hash |= 0;
    }
    return hash;
  }
}
