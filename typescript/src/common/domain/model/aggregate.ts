import type { AggregateId } from '../identity/aggregateId.js';
import type { Entity } from './entity.js';

export interface Aggregate<ID extends AggregateId> extends Entity<ID> {}
