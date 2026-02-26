package com.cotalk.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 헥사고날 아키텍처 의존성 규칙을 검증하는 ArchUnit 테스트.
 *
 * <p>레이어 의존 방향:</p>
 * <pre>
 *   Adapter (inbound/outbound)
 *       ↓
 *   Application (service)
 *       ↓
 *   Domain (entity, port, service)
 * </pre>
 *
 * <p>Infrastructure는 모든 레이어에서 독립적으로 존재하며,
 * Domain과 Application은 Infrastructure에 의존하지 않는다.</p>
 */
class HexagonalArchitectureTest {

    private static final String BASE_PACKAGE = "com.cotalk";

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Nested
    @DisplayName("Domain 레이어 규칙")
    class DomainLayerRules {

        @ParameterizedTest(name = "Domain은 {0} 레이어에 의존하지 않는다")
        @CsvSource({
                "Application, ..application..",
                "Adapter, ..adapter..",
                "Infrastructure, ..infrastructure.."
        })
        void should_notDependOnOtherLayers_when_inDomainLayer(String layerName, String forbiddenPackage) {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage(forbiddenPackage);

            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("Application 레이어 규칙")
    class ApplicationLayerRules {

        @Test
        @DisplayName("Application은 Adapter 레이어에 의존하지 않는다")
        void should_notDependOnAdapter_when_inApplicationLayer() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..");

            rule.check(classes);
        }

        @Test
        @DisplayName("Application은 Infrastructure 레이어에 의존하지 않는다")
        void should_notDependOnInfrastructure_when_inApplicationLayer() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("Adapter 레이어 규칙")
    class AdapterLayerRules {

        @Test
        @DisplayName("Inbound Adapter는 Outbound Adapter에 의존하지 않는다")
        void should_notDependOnOutbound_when_inInboundAdapter() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..adapter.inbound..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter.outbound..");

            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("순환 의존 규칙")
    class CyclicDependencyRules {

        @Test
        @DisplayName("최상위 패키지 간 순환 의존이 없다")
        void should_havNoCyclicDependencies_between_topLevelPackages() {
            ArchRule rule = slices()
                    .matching("com.cotalk.(*)..")
                    .should().beFreeOfCycles();

            rule.check(classes);
        }
    }
}
