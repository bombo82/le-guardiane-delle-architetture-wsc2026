package it.giannibombelli.wsc2026.common.utils;

import it.giannibombelli.wsc2026.common.errors.DependencyNotProvidedException;

/**
 * Helper fail-fast per garantire che argomenti e dipendenze non siano {@code null}.
 */
public final class Require {

    private Require() {
    }

    public static <T> T requireArgument(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must be defined");
        }
        return value;
    }

    public static <T> T requireDependency(T value, String name) {
        if (value == null) {
            throw new DependencyNotProvidedException(name + " must be provided");
        }
        return value;
    }
}
