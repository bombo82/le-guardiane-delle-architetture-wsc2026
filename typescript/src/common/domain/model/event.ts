import type { AggregateId } from '../identity/aggregateId.js';

export type Event<K extends string, ID extends AggregateId> = {
  readonly kind: K;
  readonly aggregateId: ID;
};
