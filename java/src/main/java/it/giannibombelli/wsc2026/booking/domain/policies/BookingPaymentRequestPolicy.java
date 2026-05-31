package it.giannibombelli.wsc2026.booking.domain.policies;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.booking.domain.events.BookingPlaced;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.model.Policy;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.application.commands.RequestPayment;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;


public class BookingPaymentRequestPolicy implements Policy<BookingPlaced, RequestPayment> {

    @Override
    public RequestPayment evaluate(BookingPlaced event) {
        Require.requireArgument(event, "Booking placed event");
        return new RequestPayment(
            EntityId.generate(PaymentId::new),
            new ClientReference(event.aggregateId().value().toString()),
            event.amount(),
            Timestamp.now()
        );
    }
}
