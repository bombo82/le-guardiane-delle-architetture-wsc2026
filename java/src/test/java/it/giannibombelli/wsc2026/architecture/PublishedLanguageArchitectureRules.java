package it.giannibombelli.wsc2026.architecture;

import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static java.lang.String.format;

/**
 * Regole a protezione del pattern Published Language + Anti-Corruption Layer.
 * <p>
 * Complementari alle regole cross-BC, che vietano a un BC di dipendere dai layer interni di un altro BC.
 */
public final class PublishedLanguageArchitectureRules {

    private PublishedLanguageArchitectureRules() {
    }

    /**
     * Garantisce che il contratto pubblicato sia stabile e non venga "contaminato" da dettagli
     * di dominio, applicazione, API o infrastruttura.
     */
    public static ArchRule publishedLanguageMustNotDependOnInternalLayers(String boundedContextPackage) {
        return noClasses()
            .that().resideInAPackage(boundedContextPackage + ".integration..")
            .should().dependOnClassesThat().resideInAnyPackage(
                boundedContextPackage + ".domain..",
                boundedContextPackage + ".application..",
                boundedContextPackage + ".api..",
                boundedContextPackage + ".infrastructure.."
            )
            .because(format("Published Language of %s must not depend on its own internal layers", boundedContextPackage));
    }

    /**
     * L'upstream pubblica un contratto senza conoscere chi lo consuma.
     */
    public static ArchRule publishedLanguageMustNotDependOnDownstream(String upstreamPackage, String downstreamPackage) {
        return noClasses()
            .that().resideInAPackage(upstreamPackage + ".integration..")
            .should().dependOnClassesThat().resideInAPackage(downstreamPackage + "..")
            .because(format("Published Language of %s must not depend on downstream %s", upstreamPackage, downstreamPackage));
    }

    /**
     * Convenzione: l'ACL risiede in {@code <downstream>.application.integration.<upstreamName>}.
     */
    public static ArchRule onlyAntiCorruptionLayerMayConsumePublishedLanguage(
        String downstreamPackage,
        String upstreamName,
        String upstreamPackage
    ) {
        return noClasses()
            .that().resideInAPackage(downstreamPackage + "..")
            .and().resideOutsideOfPackage(downstreamPackage + ".application.integration." + upstreamName + "..")
            .and().haveSimpleNameNotEndingWith("Module")
            .should().dependOnClassesThat().resideInAPackage(upstreamPackage + ".integration..")
            .because(format(
                "Only the Anti-Corruption Layer %s.application.integration.%s and the module facade may consume the Published Language of %s",
                downstreamPackage, upstreamName, upstreamPackage
            ));
    }
}
