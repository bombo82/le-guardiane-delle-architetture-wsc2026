// Caso d'uso reattivo che fa scadere un pagamento alla scadenza delle 48h.

import type { EventPublisher } from '@/common/application/events/eventPublisher.js';
import type { EventSubscriber } from '@/common/application/events/eventSubscriber.js';
import { PaymentEvent } from '../../domain/events/paymentEvent.js';
import type { PaymentDeadlineReached } from '../../domain/events/paymentDeadlineReached.js';
import type { PaymentExpired } from '../../domain/events/paymentResultEvents.js';

import { PaymentNotFoundException } from '../../domain/payment/paymentNotFoundException.js';
import { PaymentExpiration } from '../../domain/policies/paymentExpiration.js';
import type { PaymentRepository } from '../../domain/ports/paymentRepository.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class PaymentExpiring implements EventSubscriber<PaymentDeadlineReached> {
  private readonly _paymentRepository: PaymentRepository;
  private readonly _eventPublisher: EventPublisher<PaymentEvent>;
  private readonly _paymentExpiration: PaymentExpiration;

  constructor(
    paymentRepository: PaymentRepository,
    eventPublisher: EventPublisher<PaymentEvent>,
    paymentExpiration: PaymentExpiration
  ) {
    requireDependency(paymentRepository, "paymentRepository");
    requireDependency(eventPublisher, "eventPublisher");
    requireDependency(paymentExpiration, "paymentExpiration");
    this._paymentRepository = paymentRepository;
    this._eventPublisher = eventPublisher;
    this._paymentExpiration = paymentExpiration;
  }

  on(event: PaymentDeadlineReached): void {
    requireArgument(event, 'event');

    const cmd = this._paymentExpiration.evaluate(event);
    const payment = this._paymentRepository.findById(cmd.aggregateId);
    if (payment === null) throw new PaymentNotFoundException();

    const expired: PaymentExpired = payment.expire();
    this._paymentRepository.save(payment);

    this._eventPublisher.publish(expired);
  }
}
