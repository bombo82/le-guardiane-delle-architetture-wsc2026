package it.giannibombelli.wsc2026.common.errors;

/**
 * Segnala un errore di composizione dell'applicazione (dipendenza non fornita nel wiring),
 * a differenza di {@link IllegalArgumentException} che indica un input invalido del chiamante.
 */
public class DependencyNotProvidedException extends RuntimeException {

    public DependencyNotProvidedException(String message) {
        super(message);
    }
}
