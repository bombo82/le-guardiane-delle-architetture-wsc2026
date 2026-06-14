export interface EventSubscriber<E> {
  on(event: E): void;
}
