// Caso d'uso reattivo che accetta una transazione e completa il pagamento se possibile.

import type { EventPublisher } from '@/common/application/events/eventPublisher.js';
import type { EventSubscriber } from '@/common/application/events/eventSubscriber.js';
import { PaymentEvent } from '../../domain/events/paymentEvent.js';
import { TransactionAccepted } from '../../domain/events/transactionAccepted.js';
import { PaymentNotFoundException } from '../../domain/payment/paymentNotFoundException.js';
import { PaymentCompletion } from '../../domain/policies/paymentCompletion.js';
import type { PaymentRepository } from '../../domain/ports/paymentRepository.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class TransactionAccepting implements EventSubscriber<TransactionAccepted> {
  private readonly _paymentRepository: PaymentRepository;
  private readonly _eventPublisher: EventPublisher<PaymentEvent>;
  private readonly _paymentCompletion: PaymentCompletion;

  constructor(
    paymentRepository: PaymentRepository,
    eventPublisher: EventPublisher<PaymentEvent>,
    paymentCompletion: PaymentCompletion
  ) {
    requireDependency(paymentRepository, "paymentRepository");
    requireDependency(eventPublisher, "eventPublisher");
    requireDependency(paymentCompletion, "paymentCompletion");
    this._paymentRepository = paymentRepository;
    this._eventPublisher = eventPublisher;
    this._paymentCompletion = paymentCompletion;
  }

  on(event: TransactionAccepted): void {
    requireArgument(event, 'event');

    const cmd = this._paymentCompletion.evaluate(event);
    const payment = this._paymentRepository.findById(cmd.aggregateId);
    if (payment === null) throw new PaymentNotFoundException();

    const accepted = payment.acceptTransaction(cmd.transactionId, cmd.providerCompletedAt);
    this._paymentRepository.save(payment);

    if (accepted !== null) {
      this._eventPublisher.publish(accepted);
    }
  }
}
