package it.giannibombelli.wsc2026.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Regole condivise per la verifica della purezza dei layer domain/application
 * all'interno di un singolo Bounded Context.
 */
public final class DomainApplicationPurityRules {

    private static final Set<String> JAVA_WRAPPERS = Set.of(
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Boolean",
        "java.lang.Double",
        "java.lang.Float",
        "java.lang.Byte",
        "java.lang.Short",
        "java.lang.Character",
        "java.lang.Void",
        "java.lang.String"
    );

    private DomainApplicationPurityRules() {
    }

    public static ArchRule onlyProjectInternalsAndJdk(String boundedContextPackage) {
        return classes()
            .that().resideInAnyPackage(
                boundedContextPackage + ".domain..",
                boundedContextPackage + ".application.."
            )
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "java.lang..",
                "java.util..",
                "it.giannibombelli.wsc2026.."
            )
            .because("domain and application layers of " + boundedContextPackage + " should only depend on project internals and JDK");
    }

    public static ArchRule noPrimitiveOrWrapperFields(String boundedContextPackage) {
        return classes()
            .that().resideInAnyPackage(
                boundedContextPackage + ".domain..",
                boundedContextPackage + ".application.."
            )
            .and().areNotEnums()
            .should(new ArchCondition<JavaClass>("not declare non-constant primitive or wrapper fields") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    javaClass.getFields().stream()
                        .filter(field -> isPrimitiveOrWrapper(field.getRawType()))
                        .filter(field -> !isConstant(field))
                        .forEach(field -> events.add(SimpleConditionEvent.violated(javaClass,
                            javaClass.getName() + "." + field.getName()
                                + " is a " + field.getRawType().getName() + " field")));
                }

                private boolean isConstant(JavaField field) {
                    return field.getModifiers().contains(JavaModifier.STATIC)
                        && field.getModifiers().contains(JavaModifier.FINAL);
                }
            });
    }

    public static ArchRule noPrimitiveOrWrapperParameters(String boundedContextPackage) {
        return classes()
            .that().resideInAnyPackage(
                boundedContextPackage + ".domain..",
                boundedContextPackage + ".application.."
            )
            .and().areNotEnums()
            .should(new ArchCondition<JavaClass>("not use primitive or wrapper parameters") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    javaClass.getMethods().forEach(method -> checkParameters(javaClass, method, events));
                    javaClass.getConstructors().forEach(constructor -> checkParameters(javaClass, constructor, events));
                }

                private void checkParameters(JavaClass javaClass, JavaCodeUnit codeUnit, ConditionEvents events) {
                    for (JavaParameter parameter : codeUnit.getParameters()) {
                        if (isPrimitiveOrWrapper(parameter.getRawType())) {
                            events.add(SimpleConditionEvent.violated(javaClass,
                                javaClass.getName() + "." + codeUnit.getName()
                                    + " has " + parameter.getRawType().getName() + " parameter"));
                        }
                    }
                }
            });
    }

    private static boolean isPrimitiveOrWrapper(JavaClass type) {
        return type.isPrimitive() || JAVA_WRAPPERS.contains(type.getFullName());
    }
}
