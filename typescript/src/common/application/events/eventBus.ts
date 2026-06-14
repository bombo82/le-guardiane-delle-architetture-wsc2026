import type { EventPublisher } from './eventPublisher.js';
import type { EventSubscriber } from './eventSubscriber.js';

export interface EventBus<E> extends EventPublisher<E> {
  subscribe(eventKind: string, subscriber: EventSubscriber<E>): void;
}
