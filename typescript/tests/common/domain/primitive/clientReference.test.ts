import { describe, expect, it } from 'vitest';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

describe('ClientReference', () => {
  it('dovrebbe creare un riferimento valido da un Uuid', () => {
    const uuid = Uuid.generate();

    const reference = new ClientReference(uuid);

    expect(reference.value).toBe(uuid);
    expect(reference.toString()).toBe(uuid.value);
  });

  it('dovrebbe fallire se Uuid è nullo', () => {
    expect(() => new ClientReference(null as unknown as Uuid)).toThrow('clientReference must be defined');
  });
});
