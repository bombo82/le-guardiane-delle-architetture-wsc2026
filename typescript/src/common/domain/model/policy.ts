import type { Command } from '../../application/command.js';
import type { AggregateId } from '../identity/aggregateId.js';
import type { Event } from './event.js';

export interface Policy<E extends Event<string, AggregateId>, C extends Command<string, AggregateId>> {
  evaluate(event: E): C | null;
}
