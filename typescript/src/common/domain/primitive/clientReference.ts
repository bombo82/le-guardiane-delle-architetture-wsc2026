import { requireArgument } from '@/common/utils/requireArgument.js';
import type { EntityId } from '@/common/domain/identity/entityId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

export class ClientReference implements EntityId {
  readonly value: Uuid;

  constructor(value: Uuid) {
    requireArgument(value, 'clientReference');
    this.value = value;
  }

  toString(): string {
    return this.value.value;
  }
}
