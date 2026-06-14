import { describe, expect, it } from 'vitest';
import { AggregateId } from '@/common/domain/identity/aggregateId.js';
import { EntityId, generateId } from '@/common/domain/identity/entityId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

class TestEntityId implements EntityId {
  constructor(readonly value: Uuid) {
    if (value === null || value === undefined) {
      throw new Error('value must be defined');
    }
  }
}

class TestAggregateId implements AggregateId {
  constructor(readonly value: Uuid) {
    if (value === null || value === undefined) {
      throw new Error('value must be defined');
    }
  }
}

describe('EntityId', () => {
  describe('validazione id di entità', () => {
    it('dovrebbe creare un id con un valore valido', () => {
      const uuid = Uuid.fromString('550e8400-e29b-41d4-a716-446655440000');

      const id = new TestEntityId(uuid);

      expect(id.value).toBe(uuid);
    });

    it('dovrebbe fallire se il valore è nullo', () => {
      expect(() => new TestEntityId(null as unknown as Uuid)).toThrow();
    });
  });

  describe('validazione id di aggregato', () => {
    it('dovrebbe creare un id aggregato con un valore valido', () => {
      const uuid = Uuid.fromString('550e8400-e29b-41d4-a716-446655440000');

      const id = new TestAggregateId(uuid);

      expect(id.value).toBe(uuid);
    });

    it('dovrebbe fallire se il valore è nullo', () => {
      expect(() => new TestAggregateId(null as unknown as Uuid)).toThrow();
    });
  });

  describe('generazione', () => {
    it('dovrebbe generare un id non nullo', () => {
      const id = generateId((idValue: Uuid) => new TestAggregateId(idValue));

      expect(id.value).not.toBeNull();
      expect(id.value.value).toHaveLength(36);
    });
  });
});
