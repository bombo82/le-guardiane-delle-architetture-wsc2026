// Value object che rappresenta un UUID.

import { randomUUID } from 'node:crypto';
import { requireArgument } from '@/common/utils/requireArgument.js';

const UUID_REGEX = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

export class Uuid {
  readonly value: string;

  private constructor(value: string) {
    this.value = value;
  }

  static fromString(value: string): Uuid {
    requireArgument(value, 'uuid');
    if (!UUID_REGEX.test(value)) {
      throw new Error('value must be a valid UUID');
    }
    return new Uuid(value);
  }

  static generate(): Uuid {
    return new Uuid(randomUUID());
  }
}
