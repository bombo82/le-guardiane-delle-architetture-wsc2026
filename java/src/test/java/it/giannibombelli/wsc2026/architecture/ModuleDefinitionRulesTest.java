package it.giannibombelli.wsc2026.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Verifica che i moduli (facciata di ciascun Bounded Context) rispettino
 * un contratto pubblico minimale e non espongano dettagli interni o dipendenze framework.
 */
@AnalyzeClasses(packages = "it.giannibombelli.wsc2026", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleDefinitionRulesTest {

    private static final String BOOKING_BASE = "it.giannibombelli.wsc2026.booking";
    private static final String GIFTCARD_BASE = "it.giannibombelli.wsc2026.giftcard";
    private static final String PAYMENT_BASE = "it.giannibombelli.wsc2026.payment";

    @ArchTest
    static final ArchRule moduleFieldsShouldBeFinal =
        fields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Module")
            .and().areDeclaredInClassesThat().doNotHaveSimpleName("ApplicationModule")
            .should().beFinal()
            .because("module state should be immutable after construction; non-final fields indicate lifecycle issues (e.g. watcher initialized in configure())");

    @ArchTest
    static final ArchRule modulePublicMethodsMustNotReturnInternalTypes =
        methods()
            .that().arePublic()
            .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Module")
            .and().areDeclaredInClassesThat().doNotHaveSimpleName("ApplicationModule")
            .and().doNotHaveName("webApi")
            .should().notHaveRawReturnType(resideInAnyPackage(
                BOOKING_BASE + ".domain..",
                BOOKING_BASE + ".application..",
                BOOKING_BASE + ".infrastructure..",
                GIFTCARD_BASE + ".domain..",
                GIFTCARD_BASE + ".application..",
                GIFTCARD_BASE + ".infrastructure..",
                PAYMENT_BASE + ".domain..",
                PAYMENT_BASE + ".application..",
                PAYMENT_BASE + ".infrastructure.."
            ))
            .because("modules must expose only semantic subscription methods and published language; internal types must remain encapsulated (webApi is allowed because it returns a framework-specific adapter built on top of the api layer)");

    @ArchTest
    static final ArchRule moduleFacadesShouldNotDependOnFrameworkConfiguration =
        noClasses()
            .that().haveSimpleNameEndingWith("Module")
            .and().doNotHaveSimpleName("ApplicationModule")
            .should().dependOnClassesThat().resideInAnyPackage("io.javalin..")
            .because("module facade should not depend directly on framework configuration types; the framework should be kept at the infrastructure/api boundary");
}
