package it.giannibombelli.wsc2026.common;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "it.giannibombelli.wsc2026.common", importOptions = ImportOption.DoNotIncludeTests.class)
class CommonHexagonalArchitectureTest {
    @ArchTest
    static final ArchRule applicationShouldNotDependOnModule = noClasses()
        .that().resideInAPackage("it.giannibombelli.wsc2026.common.application..")
        .should().dependOnClassesThat().resideInAPackage("it.giannibombelli.wsc2026.common.module..")
        .because("the common application layer must not depend on module utilities");
}
