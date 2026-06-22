package it.giannibombelli.wsc2026.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Il composition root non deve accedere agli internals dei moduli: questi espongono solo
 * la superficie pubblica (API, Published Language, metodi di sottoscrizione semantici).
 */
@AnalyzeClasses(packages = "it.giannibombelli.wsc2026", importOptions = ImportOption.DoNotIncludeTests.class)
class CompositionRootArchitectureTest {

    private static final String BOOKING_BASE = "it.giannibombelli.wsc2026.booking";
    private static final String GIFTCARD_BASE = "it.giannibombelli.wsc2026.giftcard";
    private static final String PAYMENT_BASE = "it.giannibombelli.wsc2026.payment";

    @ArchTest
    static final ArchRule modulesMustNotExposeIntegrationHandlers =
        methods()
            .that().arePublic()
            .and().areDeclaredInClassesThat().resideInAnyPackage(
                BOOKING_BASE + "..",
                GIFTCARD_BASE + "..",
                PAYMENT_BASE + ".."
            )
            .should().notHaveRawReturnType(resideInAnyPackage(
                BOOKING_BASE + ".application.integration.payment.handlers..",
                GIFTCARD_BASE + ".application.integration.booking.handlers..",
                GIFTCARD_BASE + ".application.integration.payment.handlers.."
            ))
            .because("modules must expose only semantic subscription methods, not internal integration handlers");

    @ArchTest
    static final ArchRule compositionRootMustNotDependOnIntegrationAdapters =
        noClasses()
            .that().haveSimpleName("Application")
            .should().dependOnClassesThat().resideInAnyPackage(
                BOOKING_BASE + ".application.integration..",
                GIFTCARD_BASE + ".application.integration.."
            )
            .because("the composition root must depend only on the modules' public surface and on published language, not on their internal anti-corruption layers");
}
