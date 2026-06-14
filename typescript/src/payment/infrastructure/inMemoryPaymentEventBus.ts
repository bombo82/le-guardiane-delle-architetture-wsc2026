// Event bus in-memory specializzato per PaymentEvent.
// Supporta dispatch sincrono nei test e asincrono in produzione tramite Executor.

import type { EventBus } from '@/common/application/events/eventBus.js';
import type { EventSubscriber } from '@/common/application/events/eventSubscriber.js';
import type { PaymentEvent } from '../domain/events/paymentEvent.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export type Executor = (task: () => void) => void;

export class InMemoryPaymentEventBus implements EventBus<PaymentEvent> {
  private readonly _subscribers = new Map<string, EventSubscriber<PaymentEvent>[]>();
  private readonly _executor: Executor;

  constructor(executor: Executor) {
    requireDependency(executor, "executor");
    this._executor = executor;
  }

  publish(event: PaymentEvent): void {
    requireArgument(event, 'event');

    const list = this._subscribers.get(event.kind) ?? [];
    this._executor(() => {
      for (const subscriber of list) {
        try {
          subscriber.on(event);
        } catch (_error) {
          // Gli errori nei subscriber asincroni non devono interrompere il flusso principale.
        }
      }
    });
  }

  subscribe(eventKind: string, subscriber: EventSubscriber<PaymentEvent>): void {
    requireArgument(eventKind, 'eventKind');
    requireArgument(subscriber, 'subscriber');
    const list = this._subscribers.get(eventKind) ?? [];
    list.push(subscriber);
    this._subscribers.set(eventKind, list);
  }
}
