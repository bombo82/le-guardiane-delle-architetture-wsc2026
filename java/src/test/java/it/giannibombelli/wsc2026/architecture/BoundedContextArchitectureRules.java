package it.giannibombelli.wsc2026.architecture;

import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public final class BoundedContextArchitectureRules {

    private BoundedContextArchitectureRules() {
    }

    public static ArchRule domainMustNotDependOnOuterLayers(String boundedContextPackage) {
        return noClasses()
            .that().resideInAPackage(layerPackage(boundedContextPackage, "domain"))
            .should().dependOnClassesThat().resideInAnyPackage(
                layerPackage(boundedContextPackage, "application"),
                layerPackage(boundedContextPackage, "infrastructure"),
                layerPackage(boundedContextPackage, "api")
            )
            .because("the domain layer must not depend on application, infrastructure or api layers");
    }

    public static ArchRule applicationMustNotDependOnAdapters(String boundedContextPackage) {
        return noClasses()
            .that().resideInAPackage(layerPackage(boundedContextPackage, "application"))
            .should().dependOnClassesThat().resideInAnyPackage(
                layerPackage(boundedContextPackage, "infrastructure"),
                layerPackage(boundedContextPackage, "api")
            )
            .because("the application layer must not depend on infrastructure or api layers");
    }

    public static ArchRule infrastructureMustNotDependOnApi(String boundedContextPackage) {
        return noClasses()
            .that().resideInAPackage(layerPackage(boundedContextPackage, "infrastructure"))
            .should().dependOnClassesThat().resideInAPackage(layerPackage(boundedContextPackage, "api"))
            .because("the infrastructure layer must not depend on the api layer");
    }

    public static ArchRule apiMustNotDependOnInfrastructure(String boundedContextPackage) {
        return noClasses()
            .that().resideInAPackage(layerPackage(boundedContextPackage, "api"))
            .should().dependOnClassesThat().resideInAPackage(layerPackage(boundedContextPackage, "infrastructure"))
            .because("the api layer must not depend on the infrastructure layer");
    }

    private static String layerPackage(String boundedContextPackage, String layer) {
        return boundedContextPackage + "." + layer + "..";
    }
}
