import { describe, expect, it } from 'vitest';
import { Description } from '@/common/domain/primitive/description.js';

describe('Description', () => {
  it('should create with valid value', () => {
    const description = new Description('Nice trip');
    expect(description.value).toEqual('Nice trip');
  });

  it('should fail if null', () => {
    expect(() => new Description(null as unknown as string)).toThrow();
  });

  it('should fail if blank', () => {
    expect(() => new Description('')).toThrow();
    expect(() => new Description('   ')).toThrow();
  });
});
