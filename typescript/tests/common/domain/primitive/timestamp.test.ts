import { describe, expect, it } from 'vitest';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';

describe('Timestamp', () => {
  it('dovrebbe creare un timestamp con un valore valido', () => {
    const date = new Date('2026-06-07T10:00:00.000Z');

    const timestamp = new Timestamp(date);

    expect(timestamp.value).toEqual(date);
  });

  it('dovrebbe fallire se il valore è nullo', () => {
    expect(() => new Timestamp(null as unknown as Date)).toThrow('timestamp must be defined');
  });

  it('dovrebbe creare now', () => {
    const before = new Date();

    const timestamp = Timestamp.now();

    const after = new Date();
    expect(timestamp.value.getTime()).toBeGreaterThanOrEqual(before.getTime());
    expect(timestamp.value.getTime()).toBeLessThanOrEqual(after.getTime());
  });
});
