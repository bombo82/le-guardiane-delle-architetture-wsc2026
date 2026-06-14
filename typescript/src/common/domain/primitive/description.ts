import { requireArgument } from '@/common/utils/requireArgument.js';

export class Description {
  readonly value: string;

  constructor(value: string) {
    requireArgument(value, 'description');
    if (value.trim().length === 0) {
      throw new Error('description must not be blank');
    }
    this.value = value;
  }
}
