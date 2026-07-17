import type { AggregateId } from '../domain/identity/aggregateId.js';

export type Command<K extends string, ID extends AggregateId> = {
  readonly kind: K;
  readonly aggregateId: ID;
};
