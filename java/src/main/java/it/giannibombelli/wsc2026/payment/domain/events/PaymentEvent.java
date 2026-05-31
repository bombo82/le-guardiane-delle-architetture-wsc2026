package it.giannibombelli.wsc2026.payment.domain.events;

import it.giannibombelli.wsc2026.common.domain.model.Event;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;

public interface PaymentEvent extends Event<PaymentId> {
}
