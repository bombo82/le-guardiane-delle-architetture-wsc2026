package it.giannibombelli.wsc2026.common.domain.identity;

import java.util.UUID;
import java.util.function.Function;

public interface EntityId {
    UUID value();

    static <ID extends EntityId> ID generate(Function<UUID, ID> factory) {
        return factory.apply(UUID.randomUUID());
    }
}