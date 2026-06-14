// Caso d'uso per richiedere un rimborso.

import type { UseCase } from '@/common/application/usecase.js';
import type { EventPublisher } from '@/common/application/events/eventPublisher.js';
import { PaymentEvent } from '../../domain/events/paymentEvent.js';
import { RefundRequested } from '../../domain/events/refundRequested.js';
import { PaymentNotFoundException } from '../../domain/payment/paymentNotFoundException.js';
import type { PaymentRepository } from '../../domain/ports/paymentRepository.js';
import { RefundTransaction } from '../commands/refundTransaction.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class RefundRequesting implements UseCase<RefundTransaction, RefundRequested> {
  private readonly _paymentRepository: PaymentRepository;
  private readonly _eventPublisher: EventPublisher<PaymentEvent>;

  constructor(paymentRepository: PaymentRepository, eventPublisher: EventPublisher<PaymentEvent>) {
    requireDependency(paymentRepository, "paymentRepository");
    requireDependency(eventPublisher, "eventPublisher");
    this._paymentRepository = paymentRepository;
    this._eventPublisher = eventPublisher;
  }

  invoke(cmd: RefundTransaction): RefundRequested {
    requireArgument(cmd, 'command');

    const payment = this._paymentRepository.findById(cmd.aggregateId);
    if (payment === null) throw new PaymentNotFoundException();

    const event = payment.requestRefund(cmd.amount);
    this._paymentRepository.save(payment);

    this._eventPublisher.publish(event);
    return event;
  }
}
