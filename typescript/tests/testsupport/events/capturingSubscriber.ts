// Subscriber di test che cattura gli eventi ricevuti.

import type { EventSubscriber } from '@/common/application/events/eventSubscriber.js';

export class CapturingSubscriber<T> implements EventSubscriber<T> {
  private readonly _events: T[] = [];

  on(event: T): void {
    this._events.push(event);
  }

  events(): T[] {
    return [...this._events];
  }
}
