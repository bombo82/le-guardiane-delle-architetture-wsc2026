package it.giannibombelli.wsc2026.common.module;

import io.javalin.config.JavalinConfig;

/**
 * Contratto comune per gli adapter web di ciascun Bounded Context.
 * Il Composition Root chiama {@code configure(...)} per montare le rotte sul framework HTTP.
 */
public interface WebApi {
    void configure(JavalinConfig config);
}
