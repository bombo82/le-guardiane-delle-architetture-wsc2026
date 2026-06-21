// Caso d'uso reattivo che rifiuta una transazione e marca il pagamento come REJECTED.

import type { EventPublisher } from '@/common/application/events/eventPublisher.js';
import type { EventSubscriber } from '@/common/application/events/eventSubscriber.js';
import { PaymentEvent } from '../../domain/events/paymentEvent.js';
import type { PaymentRejected } from '../../domain/events/paymentResultEvents.js';

import { TransactionRejected } from '../../domain/events/transactionRejected.js';
import { PaymentNotFoundException } from '../../domain/payment/paymentNotFoundException.js';
import { PaymentRejection } from '../../application/policies/paymentRejection.js';
import type { PaymentRepository } from '../../domain/ports/paymentRepository.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class TransactionRejecting implements EventSubscriber<TransactionRejected> {
  private readonly _paymentRepository: PaymentRepository;
  private readonly _eventPublisher: EventPublisher<PaymentEvent>;
  private readonly _paymentRejection: PaymentRejection;

  constructor(
    paymentRepository: PaymentRepository,
    eventPublisher: EventPublisher<PaymentEvent>,
    paymentRejection: PaymentRejection
  ) {
    requireDependency(paymentRepository, "paymentRepository");
    requireDependency(eventPublisher, "eventPublisher");
    requireDependency(paymentRejection, "paymentRejection");
    this._paymentRepository = paymentRepository;
    this._eventPublisher = eventPublisher;
    this._paymentRejection = paymentRejection;
  }

  on(event: TransactionRejected): void {
    requireArgument(event, 'event');

    const cmd = this._paymentRejection.evaluate(event);
    const payment = this._paymentRepository.findById(cmd.aggregateId);
    if (payment === null) throw new PaymentNotFoundException();

    const rejected: PaymentRejected = payment.rejectTransaction(cmd.transactionId, cmd.reason);
    this._paymentRepository.save(payment);

    this._eventPublisher.publish(rejected);
  }
}
