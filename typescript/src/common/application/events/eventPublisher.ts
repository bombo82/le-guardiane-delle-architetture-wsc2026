export interface EventPublisher<E> {
  publish(event: E): void;
}
