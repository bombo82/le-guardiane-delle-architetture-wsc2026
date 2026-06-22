package it.giannibombelli.wsc2026.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.Arrays;
import java.util.List;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "it.giannibombelli.wsc2026", importOptions = ImportOption.DoNotIncludeTests.class)
class CrossBoundedContextDependenciesTest {
    final static String COMMON_PACKAGE = "..common..";

    @ArchTest
    static final ArchRule bookingMustNotDependOnOtherBoundedContexts = noClasses()
        .that().resideInAPackage(BoundedContext.BOOKING.getPackagePattern())
        .should().dependOnClassesThat(otherBoundedContextsExceptIntegration(BoundedContext.BOOKING))
        .because("Booking BC must not depend on GiftCard or Payment BC internal layers (integration packages excluded)");

    @ArchTest
    static final ArchRule giftCardMustNotDependOnOtherBoundedContexts = noClasses()
        .that().resideInAPackage(BoundedContext.GIFTCARD.getPackagePattern())
        .should().dependOnClassesThat(otherBoundedContextsExceptIntegration(BoundedContext.GIFTCARD))
        .because("GiftCard BC must not depend on Booking or Payment BC internal layers (integration packages excluded)");

    @ArchTest
    static final ArchRule paymentMustNotDependOnOtherBoundedContexts = noClasses()
        .that().resideInAPackage(BoundedContext.PAYMENT.getPackagePattern())
        .should().dependOnClassesThat(otherBoundedContextsExceptIntegration(BoundedContext.PAYMENT))
        .because("Payment BC must not depend on Booking or GiftCard BC internal layers (integration packages excluded)");

    @ArchTest
    static final ArchRule commonMustNotDependOnBoundedContexts = noClasses()
        .that().resideInAPackage(COMMON_PACKAGE)
        .should().dependOnClassesThat().resideInAnyPackage(getBoundedContextPackages())
        .because("Common shared code must not depend on any bounded context");

    enum BoundedContext {
        BOOKING("it.giannibombelli.wsc2026.booking.."),
        GIFTCARD("it.giannibombelli.wsc2026.giftcard.."),
        PAYMENT("it.giannibombelli.wsc2026.payment..");

        private final String packagePattern;

        BoundedContext(String packagePattern) {
            this.packagePattern = packagePattern;
        }

        public String getPackagePattern() {
            return packagePattern;
        }

    }

    private static DescribedPredicate<JavaClass> otherBoundedContextsExceptIntegration(BoundedContext self) {
        List<BoundedContext> others = Arrays.stream(BoundedContext.values())
            .filter(bc -> bc != self)
            .toList();

        DescribedPredicate<JavaClass> inOtherBoundedContexts = others.stream()
            .map(bc -> resideInAPackage(bc.getPackagePattern()))
            .reduce((a, b) -> a.or(b))
            .orElseThrow();

        DescribedPredicate<JavaClass> notInIntegration = others.stream()
            .map(bc -> not(resideInAPackage(bc.getPackagePattern() + "integration..")))
            .reduce((a, b) -> a.and(b))
            .orElseThrow();

        return inOtherBoundedContexts.and(notInIntegration);
    }

    private static String[] getBoundedContextPackages() {
        return Arrays.stream(BoundedContext.values())
            .map(BoundedContext::getPackagePattern)
            .toArray(String[]::new);
    }
}
