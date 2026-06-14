import { requireArgument } from '@/common/utils/requireArgument.js';

export class ClientReference {
  readonly value: string;

  constructor(value: string) {
    requireArgument(value, 'clientReference');
    if (value.trim().length === 0) {
      throw new Error('clientReference must not be blank');
    }
    this.value = value;
  }
}
