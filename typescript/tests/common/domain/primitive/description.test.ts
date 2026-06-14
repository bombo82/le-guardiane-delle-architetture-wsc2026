import { describe, expect, it } from 'vitest';
import { Description } from '@/common/domain/primitive/description.js';

describe('Description', () => {
  it('dovrebbe creare una descrizione valida', () => {
    const description = new Description('Descrizione di test');

    expect(description.value).toBe('Descrizione di test');
  });

  it('dovrebbe fallire se il valore è nullo', () => {
    expect(() => new Description(null as unknown as string)).toThrow('description must be defined');
  });

  it('dovrebbe fallire se il valore è blank', () => {
    expect(() => new Description('')).toThrow('description must not be blank');
    expect(() => new Description('   ')).toThrow('description must not be blank');
  });
});
