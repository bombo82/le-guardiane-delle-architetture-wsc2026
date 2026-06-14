import { describe, expect, it } from 'vitest';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { ProviderReference } from '@/payment/domain/payment/providerReference.js';

describe('ProviderReference', () => {
  it('should create with valid value', () => {
    const uuid = Uuid.generate();

    const reference = new ProviderReference(uuid);

    expect(reference.value).toEqual(uuid);
  });

  it('should fail if null', () => {
    expect(() => new ProviderReference(null as unknown as Uuid)).toThrow();
  });
});
