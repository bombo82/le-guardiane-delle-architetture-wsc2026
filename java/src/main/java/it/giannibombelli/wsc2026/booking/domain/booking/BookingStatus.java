package it.giannibombelli.wsc2026.booking.domain.booking;

public enum BookingStatus {
    PLACED("placed"),
    CONFIRMED("confirmed"),
    REFUSED("refused"),
    REJECTED("rejected");

    private final String value;

    BookingStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
