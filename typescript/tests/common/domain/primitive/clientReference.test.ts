import { describe, expect, it } from 'vitest';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';

describe('ClientReference', () => {
  it('dovrebbe creare un riferimento valido', () => {
    const reference = new ClientReference('client-123');

    expect(reference.value).toBe('client-123');
  });

  it('dovrebbe fallire se il valore è nullo', () => {
    expect(() => new ClientReference(null as unknown as string)).toThrow('clientReference must be defined');
  });

  it('dovrebbe fallire se il valore è blank', () => {
    expect(() => new ClientReference('')).toThrow('clientReference must not be blank');
    expect(() => new ClientReference('   ')).toThrow('clientReference must not be blank');
  });
});
