// Riferimento opaco: ha significato solo per il provider, payment non lo interpreta.

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
