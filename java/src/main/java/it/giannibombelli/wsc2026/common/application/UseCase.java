package it.giannibombelli.wsc2026.common.application;

import it.giannibombelli.wsc2026.common.domain.model.Event;

public interface UseCase<C extends Command<?>, E extends Event<?>> {
    E invoke(C cmd);
}
