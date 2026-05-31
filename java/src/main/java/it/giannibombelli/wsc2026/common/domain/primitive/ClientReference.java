package it.giannibombelli.wsc2026.common.domain.primitive;

public record ClientReference(String value) {
    public ClientReference {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("clientReference must not be null or blank");
        }
    }
}
