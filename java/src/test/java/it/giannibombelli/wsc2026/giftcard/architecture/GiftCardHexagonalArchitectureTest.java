package it.giannibombelli.wsc2026.giftcard.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static it.giannibombelli.wsc2026.architecture.BoundedContextArchitectureRules.*;

@AnalyzeClasses(packages = "it.giannibombelli.wsc2026.giftcard", importOptions = ImportOption.DoNotIncludeTests.class)
class GiftCardHexagonalArchitectureTest {

    private static final String BC = "it.giannibombelli.wsc2026.giftcard";

    @ArchTest
    static final ArchRule domainMustNotDependOnOuterLayers = domainMustNotDependOnOuterLayers(BC);

    @ArchTest
    static final ArchRule applicationMustNotDependOnAdapters = applicationMustNotDependOnAdapters(BC);

    @ArchTest
    static final ArchRule infrastructureMustNotDependOnApi = infrastructureMustNotDependOnApi(BC);

    @ArchTest
    static final ArchRule apiMustNotDependOnInfrastructure = apiMustNotDependOnInfrastructure(BC);
}
