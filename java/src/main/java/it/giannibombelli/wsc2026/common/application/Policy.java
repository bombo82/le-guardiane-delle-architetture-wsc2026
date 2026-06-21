package it.giannibombelli.wsc2026.common.application;

import it.giannibombelli.wsc2026.common.application.Command;
import it.giannibombelli.wsc2026.common.domain.model.Event;

public interface Policy<E extends Event<?>, C extends Command<?>> {
    C evaluate(E event);
}
