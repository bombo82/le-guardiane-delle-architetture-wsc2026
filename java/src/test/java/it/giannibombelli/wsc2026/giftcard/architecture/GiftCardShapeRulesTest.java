package it.giannibombelli.wsc2026.giftcard.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static it.giannibombelli.wsc2026.architecture.BoundedContextShapeRules.commandsMustImplementCommand;
import static it.giannibombelli.wsc2026.architecture.BoundedContextShapeRules.policiesMustImplementPolicy;
import static it.giannibombelli.wsc2026.architecture.BoundedContextShapeRules.useCasesMustImplementUseCase;

@AnalyzeClasses(packages = "it.giannibombelli.wsc2026.giftcard", importOptions = ImportOption.DoNotIncludeTests.class)
class GiftCardShapeRulesTest {

    private static final String BC = "it.giannibombelli.wsc2026.giftcard";

    @ArchTest
    static final ArchRule commandsMustImplementCommandRule = commandsMustImplementCommand(BC);

    @ArchTest
    static final ArchRule policiesMustImplementPolicyRule = policiesMustImplementPolicy(BC);

    @ArchTest
    static final ArchRule useCasesMustImplementUseCaseRule = useCasesMustImplementUseCase(BC);
}
