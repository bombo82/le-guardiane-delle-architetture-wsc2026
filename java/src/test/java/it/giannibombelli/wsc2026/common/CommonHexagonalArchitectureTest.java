package it.giannibombelli.wsc2026.common;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "it.giannibombelli.wsc2026.common", importOptions = ImportOption.DoNotIncludeTests.class)
class CommonHexagonalArchitectureTest {
    final static String COMMON_PACKAGE = "it.giannibombelli.wsc2026.common";
    final static String COMMON_DOMAIN_PACKAGE = COMMON_PACKAGE + ".domain..";
    final static String COMMON_APPLICATION_PACKAGE = COMMON_PACKAGE + ".application..";
    final static String COMMON_MODULE_PACKAGE = COMMON_PACKAGE + ".module..";

    @ArchTest
    static final ArchRule domainShouldNotDependOnApplicationOrModule = noClasses()
        .that().resideInAPackage(COMMON_DOMAIN_PACKAGE)
        .should().dependOnClassesThat().resideInAnyPackage(COMMON_APPLICATION_PACKAGE, COMMON_MODULE_PACKAGE)
        .because("the common domain layer must not depend on the common application or module utilities");

    @ArchTest
    static final ArchRule applicationShouldNotDependOnModule = noClasses()
        .that().resideInAPackage(COMMON_APPLICATION_PACKAGE)
        .should().dependOnClassesThat().resideInAPackage(COMMON_MODULE_PACKAGE)
        .because("the common application layer must not depend on module utilities");

}
