// Event bus in-memory specializzato per BookingEvent.
// Supporta dispatch sincrono nei test e configurabile in produzione tramite Executor.

import type { EventBus } from '@/common/application/events/eventBus.js';
import type { EventSubscriber } from '@/common/application/events/eventSubscriber.js';
import type { BookingEvent } from '../domain/events/bookingEvent.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export type Executor = (task: () => void) => void;

export class InMemoryBookingEventBus implements EventBus<BookingEvent> {
  private readonly _subscribers = new Map<string, EventSubscriber<BookingEvent>[]>();
  private readonly _executor: Executor;

  constructor(executor: Executor) {
    requireDependency(executor, "executor");
    this._executor = executor;
  }

  publish(event: BookingEvent): void {
    requireArgument(event, 'event');

    const list = this._subscribers.get(event.kind) ?? [];
    this._executor(() => list.forEach((subscriber) => subscriber.on(event)));
  }

  subscribe(eventKind: string, subscriber: EventSubscriber<BookingEvent>): void {
    requireArgument(eventKind, 'eventKind');
    requireArgument(subscriber, 'subscriber');
    const list = this._subscribers.get(eventKind) ?? [];
    list.push(subscriber);
    this._subscribers.set(eventKind, list);
  }
}
