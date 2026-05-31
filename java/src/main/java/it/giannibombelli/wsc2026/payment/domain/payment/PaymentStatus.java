package it.giannibombelli.wsc2026.payment.domain.payment;

public enum PaymentStatus {
    REQUESTED("requested"),
    PROCESSING("processing"),
    ACCEPTED("accepted"),
    REJECTED("rejected"),
    EXPIRED("expired"),
    REFUNDED("refunded");

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
