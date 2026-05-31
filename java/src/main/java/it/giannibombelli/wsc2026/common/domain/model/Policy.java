package it.giannibombelli.wsc2026.common.domain.model;

import it.giannibombelli.wsc2026.common.application.Command;

public interface Policy<E extends Event<?>, C extends Command<?>> {
    C evaluate(E event);
}
