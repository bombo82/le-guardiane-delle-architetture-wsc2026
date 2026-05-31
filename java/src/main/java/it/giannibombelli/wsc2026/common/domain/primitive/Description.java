package it.giannibombelli.wsc2026.common.domain.primitive;

public record Description(String value) {
    public Description {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("description must not be null or blank");
        }
    }
}
