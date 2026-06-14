// Publisher di test che cattura gli eventi pubblicati.

import type { EventPublisher } from '@/common/application/events/eventPublisher.js';

export class CapturingEventPublisher<T> implements EventPublisher<T> {
  private readonly _events: T[] = [];

  publish(event: T): void {
    if (event === null || event === undefined) throw new Error('event must be defined');
    this._events.push(event);
  }

  events(): T[] {
    return [...this._events];
  }
}
