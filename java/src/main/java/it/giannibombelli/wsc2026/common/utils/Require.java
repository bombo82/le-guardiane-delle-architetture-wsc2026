package it.giannibombelli.wsc2026.common.utils;

import it.giannibombelli.wsc2026.common.errors.DependencyNotProvidedException;

/**
 * Helper fail-fast per garantire che argomenti e dipendenze non siano {@code null}.
 */
public final class Require {

    private Require() {
        // utility class
    }

    /**
     * Verifica che {@code value} non sia {@code null}, lanciando
     * {@link IllegalArgumentException} con il messaggio {@code "<name> must be defined"}.
     * Da usare per parametri di metodi, costruttori, record command/event e value object.
     *
     * @param value il valore da controllare
     * @param name  il nome logico del parametro, usato nel messaggio d'errore
     * @return {@code value} se non nullo
     * @throws IllegalArgumentException se {@code value} è nullo
     */
    public static <T> T requireArgument(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must be defined");
        }
        return value;
    }

    /**
     * Verifica che {@code value} non sia {@code null}, lanciando
     * {@link DependencyNotProvidedException} con il messaggio {@code "<name> must be provided"}.
     * Da usare per dipendenze iniettate nel wiring dei moduli e dei servizi.
     *
     * @param value il valore da controllare
     * @param name  il nome logico della dipendenza, usato nel messaggio d'errore
     * @return {@code value} se non nullo
     * @throws DependencyNotProvidedException se {@code value} è nullo
     */
    public static <T> T requireDependency(T value, String name) {
        if (value == null) {
            throw new DependencyNotProvidedException(name + " must be provided");
        }
        return value;
    }
}
