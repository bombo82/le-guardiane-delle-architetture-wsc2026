import { IllegalArgumentError } from '@/common/errors/illegalArgumentError.js';

export function requireArgument<T>(value: T, name: string): asserts value is NonNullable<T> {
  if (value === null || value === undefined) {
    throw new IllegalArgumentError(`${name} must be defined`);
  }
}
