package it.giannibombelli.wsc2026.e2e.support;

import it.giannibombelli.wsc2026.booking.api.BookingResponse;
import it.giannibombelli.wsc2026.booking.api.PlaceBookingRequest;
import it.giannibombelli.wsc2026.giftcard.api.GiftCardResponse;
import it.giannibombelli.wsc2026.payment.api.PaymentDetailsResponse;
import it.giannibombelli.wsc2026.payment.api.TransactionResponse;
import it.giannibombelli.wsc2026.testsupport.JavalinTestHelper;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public final class E2EApiDriver {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final JavalinTestHelper javalin;

    public E2EApiDriver(JavalinTestHelper javalin) {
        this.javalin = javalin;
    }

    public GiftCardResponse issueGiftCard() {
        JavalinTestHelper.TestResponse response = javalin.post("/gift-cards");
        assertThat(response.status()).isEqualTo(201);
        GiftCardResponse giftCard = readValue(response, GiftCardResponse.class);
        assertThat(giftCard.id()).isNotNull();
        return giftCard;
    }

    public GiftCardResponse requestTopUp(UUID giftCardId, BigDecimal amount) {
        String body = String.format("{\"amount\": \"%s\"}", amount);
        JavalinTestHelper.TestResponse response = javalin.post("/gift-cards/" + giftCardId + "/top-up", body);
        assertThat(response.status()).isEqualTo(200);
        return readValue(response, GiftCardResponse.class);
    }

    public BookingResponse placeBooking(UUID giftCardId, BigDecimal amount) {
        PlaceBookingRequest request = new PlaceBookingRequest(amount, "E2E booking", giftCardId);
        String body = writeValue(request);
        JavalinTestHelper.TestResponse response = javalin.post("/bookings", body);
        assertThat(response.status()).isEqualTo(201);
        BookingResponse booking = readValue(response, BookingResponse.class);
        assertThat(booking.id()).isNotNull();
        assertThat(booking.giftCardId()).isEqualTo(giftCardId);
        return booking;
    }

    public TransactionResponse startFullPaymentTransaction(UUID paymentId, String provider, BigDecimal amount) {
        String body = String.format(
            "{\"provider\": \"%s\", \"providerReference\": \"%s\", \"amount\": \"%s\"}",
            provider,
            UUID.randomUUID(),
            amount
        );
        JavalinTestHelper.TestResponse response = javalin.post("/payments/" + paymentId + "/transactions", body);
        assertThat(response.status()).isEqualTo(202);
        return readValue(response, TransactionResponse.class);
    }

    public PaymentDetailsResponse findPaymentRequestedFor(String clientReference) {
        JavalinTestHelper.TestResponse response = javalin.get("/internals/payments?clientReference=" + clientReference);
        assertThat(response.status()).isEqualTo(200);
        PaymentDetailsResponse payment = readValue(response, PaymentDetailsResponse.class);
        assertThat(payment.status()).isEqualTo("REQUESTED");
        return payment;
    }

    public PaymentDetailsResponse getPaymentSummary(UUID paymentId) {
        JavalinTestHelper.TestResponse response = javalin.get("/payments/" + paymentId);
        assertThat(response.status()).isEqualTo(200);
        return readValue(response, PaymentDetailsResponse.class);
    }

    public GiftCardResponse getGiftCardSummary(UUID giftCardId) {
        JavalinTestHelper.TestResponse response = javalin.get("/gift-cards/" + giftCardId);
        assertThat(response.status()).isEqualTo(200);
        return readValue(response, GiftCardResponse.class);
    }

    public GiftCardResponse waitForGiftCardBalanceToEqual(UUID giftCardId, BigDecimal expectedBalance) {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            GiftCardResponse card = getGiftCardSummary(giftCardId);
            if (card.balance().compareTo(expectedBalance) == 0) {
                return card;
            }
            sleep(100);
        }
        throw new AssertionError("Gift card balance was not updated to " + expectedBalance + " within timeout");
    }

    public void assertPaymentMatches(PaymentDetailsResponse payment, UUID expectedId, String expectedClientReference, BigDecimal expectedAmount) {
        assertThat(payment.id()).isEqualTo(expectedId);
        assertThat(payment.clientReference()).isEqualTo(expectedClientReference);
        assertThat(payment.amount()).isEqualByComparingTo(expectedAmount);
        assertThat(payment.status()).isEqualTo("REQUESTED");
    }

    private <T> T readValue(JavalinTestHelper.TestResponse response, Class<T> clazz) {
        try {
            return JSON.readValue(response.body(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response body: " + response.body(), e);
        }
    }

    private String writeValue(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
