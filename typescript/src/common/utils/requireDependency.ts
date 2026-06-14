import { DependencyNotProvidedError } from '@/common/errors/dependencyNotProvidedError.js';

export function requireDependency<T>(value: T, name: string): asserts value is NonNullable<T> {
  if (value === null || value === undefined) {
    throw new DependencyNotProvidedError(`${name} must be provided`);
  }
}
