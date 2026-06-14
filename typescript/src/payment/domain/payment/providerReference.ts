// Riferimento opaco fornito dal provider di pagamento.

import { requireArgument } from '@/common/utils/requireArgument.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

export class ProviderReference {
  readonly value: Uuid;

  constructor(value: Uuid) {
    requireArgument(value, 'providerReference');
    this.value = value;
  }

  toString(): string {
    return this.value.value;
  }
}
