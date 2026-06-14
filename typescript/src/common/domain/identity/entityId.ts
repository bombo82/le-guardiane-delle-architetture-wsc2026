import { Uuid } from '@/common/domain/primitive/uuid.js';

export interface EntityId {
  readonly value: Uuid;
}

export function generateEntityId(): Uuid {
  return Uuid.generate();
}

export function generateId<T>(factory: (id: Uuid) => T): T {
  return factory(generateEntityId());
}
