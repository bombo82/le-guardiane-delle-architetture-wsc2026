package it.giannibombelli.wsc2026.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.Arrays;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "it.giannibombelli.wsc2026", importOptions = ImportOption.DoNotIncludeTests.class)
class CrossBoundedContextDependenciesTest {
    final static String COMMON_PACKAGE = "..common..";

    @ArchTest
    static final ArchRule bookingMustNotDependOnOtherBoundedContexts = noClasses()
        .that().resideInAPackage(BoundedContext.BOOKING.getPackagePattern())
        .should().dependOnClassesThat().resideInAnyPackage(getForbiddenPackagesFor(BoundedContext.BOOKING))
        .because("Booking BC must not depend on GiftCard or Payment BC");

    @ArchTest
    static final ArchRule giftCardMustNotDependOnOtherBoundedContexts = noClasses()
        .that().resideInAPackage(BoundedContext.GIFTCARD.getPackagePattern())
        .should().dependOnClassesThat().resideInAnyPackage(getForbiddenPackagesFor(BoundedContext.GIFTCARD))
        .because("GiftCard BC must not depend on Booking or Payment BC");

    @ArchTest
    static final ArchRule paymentMustNotDependOnOtherBoundedContexts = noClasses()
        .that().resideInAPackage(BoundedContext.PAYMENT.getPackagePattern())
        .should().dependOnClassesThat().resideInAnyPackage(getForbiddenPackagesFor(BoundedContext.PAYMENT))
        .because("Payment BC must not depend on Booking or GiftCard BC");

    @ArchTest
    static final ArchRule commonMustNotDependOnBoundedContexts = noClasses()
        .that().resideInAPackage(COMMON_PACKAGE)
        .should().dependOnClassesThat().resideInAnyPackage(getBoundedContextPackages())
        .because("Common shared code must not depend on any bounded context");

    enum BoundedContext {
        BOOKING("..booking.."),
        GIFTCARD("..giftcard.."),
        PAYMENT("..payment..");

        private final String packagePattern;

        BoundedContext(String packagePattern) {
            this.packagePattern = packagePattern;
        }

        public String getPackagePattern() {
            return packagePattern;
        }

    }

    private static String[] getForbiddenPackagesFor(BoundedContext boundedContext) {
        return Arrays.stream(BoundedContext.values())
            .filter(bc -> bc != boundedContext)
            .map(BoundedContext::getPackagePattern).toArray(String[]::new);
    }

    private static String[] getBoundedContextPackages() {
        return Arrays.stream(BoundedContext.values())
            .map(BoundedContext::getPackagePattern)
            .toArray(String[]::new);
    }
}
