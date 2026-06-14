// Astrazione di base per i Casi d'Uso dell'applicazione.

import type { AggregateId } from '../domain/identity/aggregateId.js';
import type { Event } from '../domain/model/event.js';

export interface UseCase<C, E extends Event<string, AggregateId>> {
  invoke(command: C): E;
}
