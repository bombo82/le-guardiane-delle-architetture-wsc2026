package it.giannibombelli.wsc2026.payment.domain.payment;

public enum TransactionStatus {
    STARTED("started"),
    ACCEPTED("accepted"),
    REJECTED("rejected"),
    REFUNDED("refunded");

    private final String value;

    TransactionStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
