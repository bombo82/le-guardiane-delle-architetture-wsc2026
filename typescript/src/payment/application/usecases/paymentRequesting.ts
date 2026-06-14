// Caso d'uso per richiedere un nuovo pagamento.

import type { UseCase } from '@/common/application/usecase.js';
import type { EventPublisher } from '@/common/application/events/eventPublisher.js';
import { PaymentRequested, paymentRequested } from '../../domain/events/paymentRequested.js';
import type { PaymentEvent } from '../../domain/events/paymentEvent.js';
import { Payment } from '../../domain/payment/payment.js';
import type { PaymentRepository } from '../../domain/ports/paymentRepository.js';
import { RequestPayment } from '../commands/requestPayment.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class PaymentRequesting implements UseCase<RequestPayment, PaymentRequested> {
  private readonly _paymentRepository: PaymentRepository;
  private readonly _eventPublisher: EventPublisher<PaymentEvent>;

  constructor(paymentRepository: PaymentRepository, eventPublisher: EventPublisher<PaymentEvent>) {
    requireDependency(paymentRepository, "paymentRepository");
    requireDependency(eventPublisher, "eventPublisher");
    this._paymentRepository = paymentRepository;
    this._eventPublisher = eventPublisher;
  }

  invoke(cmd: RequestPayment): PaymentRequested {
    requireArgument(cmd, 'command');

    const payment = Payment.request(cmd.aggregateId, cmd.clientReference, cmd.amount, cmd.requestedAt);

    this._paymentRepository.save(payment);
    const event = paymentRequested(payment.id(), payment.clientReference(), payment.amount());
    this._eventPublisher.publish(event);
    return event;
  }
}
