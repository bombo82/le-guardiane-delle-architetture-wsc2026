package it.giannibombelli.wsc2026.architecture;

import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static java.lang.String.format;

/**
 * Regole architetturali per proteggere il pattern Published Language + Anti-Corruption Layer.
 * <p>
 * Queste regole sono complementari alle regole cross-BC: mentre quelle vietano a un BC di dipendere
 * dai layer interni di un altro BC, queste garantiscono che:
 * <ul>
 *   <li>la Published Language di un BC non dipenda dai layer interni dello stesso BC;</li>
 *   <li>la Published Language di un BC non dipenda dal BC downstream;</li>
 *   <li>solo l'Anti-Corruption Layer del downstream possa consumare la Published Language upstream.</li>
 * </ul>
 */
public final class PublishedLanguageArchitectureRules {

    private PublishedLanguageArchitectureRules() {
    }

    /**
     * La Published Language di un BC non deve dipendere dai layer interni dello stesso BC.
     * <p>
     * Questo garantisce che il contratto pubblicato sia stabile e non venga "contaminato" da dettagli
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
     * La Published Language di un BC upstream non deve dipendere dal BC downstream.
     * <p>
     * L'upstream pubblica un contratto senza conoscere chi lo consuma.
     */
    public static ArchRule publishedLanguageMustNotDependOnDownstream(String upstreamPackage, String downstreamPackage) {
        return noClasses()
            .that().resideInAPackage(upstreamPackage + ".integration..")
            .should().dependOnClassesThat().resideInAPackage(downstreamPackage + "..")
            .because(format("Published Language of %s must not depend on downstream %s", upstreamPackage, downstreamPackage));
    }

    /**
     * Solo l'Anti-Corruption Layer di un BC downstream può consumare la Published Language di un upstream.
     * <p>
     * L'ACL risiede in {@code <downstream>.application.integration.<upstreamName>}.
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
