// Watcher schedulato che pubblica PaymentDeadlineReached per i pagamenti scaduti.

import type { EventPublisher } from '@/common/application/events/eventPublisher.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { paymentDeadlineReached } from '../domain/events/paymentDeadlineReached.js';
import type { PaymentEvent } from '../domain/events/paymentEvent.js';
import { PaymentExpiration } from '../domain/policies/paymentExpiration.js';
import type { PaymentRepository } from '../domain/ports/paymentRepository.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

const FIVE_MINUTES_MS = 5 * 60 * 1000;
const DEADLINE_WINDOW_SECONDS = 48 * 3600;

export class PaymentDeadlineWatcher {
  private readonly _repository: PaymentRepository;
  private readonly _policy: PaymentExpiration;
  private readonly _eventPublisher: EventPublisher<PaymentEvent>;
  private _interval: ReturnType<typeof setInterval> | null = null;

  constructor(repository: PaymentRepository, policy: PaymentExpiration, eventPublisher: EventPublisher<PaymentEvent>) {
    requireArgument(repository, 'repository');
    requireDependency(policy, "policy");
    requireDependency(eventPublisher, "eventPublisher");
    this._repository = repository;
    this._policy = policy;
    this._eventPublisher = eventPublisher;
  }

  start(): void {
    this._interval = setInterval(() => this.checkDeadlines(), FIVE_MINUTES_MS);
  }

  private checkDeadlines(): void {
    const now = Timestamp.now();
    const threshold = now.minusSeconds(DEADLINE_WINDOW_SECONDS);
    const candidates = this._repository.findAllRequestedAndProcessingBefore(threshold);
    for (const payment of candidates) {
      if (this._policy.isDeadlineReached(payment, now)) {
        this._eventPublisher.publish(paymentDeadlineReached(payment.id()));
      }
    }
  }

  stop(): void {
    if (this._interval !== null) {
      clearInterval(this._interval);
      this._interval = null;
    }
  }
}
