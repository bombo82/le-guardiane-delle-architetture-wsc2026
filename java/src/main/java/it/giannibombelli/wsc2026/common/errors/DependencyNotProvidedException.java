package it.giannibombelli.wsc2026.common.errors;

/**
 * Eccezione lanciata quando una dipendenza richiesta non è stata fornita al momento
 * del wiring/dell'injection. A differenza di {@link IllegalArgumentException},
 * che segnala un input invalido fornito dal chiamante, questa eccezione indica un
 * errore di configurazione/composizione dell'applicazione.
 */
public class DependencyNotProvidedException extends RuntimeException {

    public DependencyNotProvidedException(String message) {
        super(message);
    }
}
