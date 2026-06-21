import type { Command } from './command.js';
import type { AggregateId } from '../domain/identity/aggregateId.js';
import type { Event } from '../domain/model/event.js';

export interface Policy<E extends Event<string, AggregateId>, C extends Command<string, AggregateId>> {
  evaluate(event: E): C | null;
}
