package com.loanapp;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture constraint tests derived from architecture.json.
 *
 * Each test enforces one "deny" constraint from the java perspective.
 * Violations appear in SonarCloud as named test failures under the
 * Structure and Relationships panels.
 *
 * Layer packages:
 *   controller  → com.loanapp.controller
 *   service     → com.loanapp.service
 *   repository  → com.loanapp.repository
 *   model       → com.loanapp.model
 *   dto         → com.loanapp.dto
 *   mapper      → com.loanapp.mapper
 *   config      → com.loanapp.config
 */
class ArchitectureRulesTest {

    private static final String BASE_PACKAGE = "com.loanapp";

    private static final String CONTROLLER  = "com.loanapp.controller..";
    private static final String SERVICE     = "com.loanapp.service..";
    private static final String REPOSITORY  = "com.loanapp.repository..";
    private static final String MODEL       = "com.loanapp.model..";
    private static final String DTO         = "com.loanapp.dto..";
    private static final String MAPPER      = "com.loanapp.mapper..";
    private static final String CONFIG      = "com.loanapp.config..";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SERVICE constraints
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Service layer constraints")
    class ServiceConstraints {

        @Test
        @DisplayName("[ARCH-DRIFT-01/02] Service must never depend on controller — prevents reverse dependency cycle")
        void service_must_not_depend_on_controller() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(SERVICE)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
                    .because("Service layer must never call back into controller — " +
                             "prevents reverse dependency cycle (ARCH-DRIFT-01, ARCH-DRIFT-02)");

            rule.check(classes);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REPOSITORY constraints
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Repository layer constraints")
    class RepositoryConstraints {

        @Test
        @DisplayName("[ARCH-DRIFT-06] Repository must never depend on service — data layer must remain unaware of business logic")
        void repository_must_not_depend_on_service() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(REPOSITORY)
                    .should().dependOnClassesThat().resideInAPackage(SERVICE)
                    .because("Repository must never call service — data layer must remain " +
                             "unaware of business logic (ARCH-DRIFT-06)");

            rule.check(classes);
        }

        @Test
        @DisplayName("Repository must never depend on controller — no upward dependency from data layer")
        void repository_must_not_depend_on_controller() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(REPOSITORY)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
                    .because("Repository must never call controller — " +
                             "no upward dependency from data layer");

            rule.check(classes);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODEL constraints
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Model (entity) layer constraints")
    class ModelConstraints {

        @Test
        @DisplayName("Model must not depend on controller — keeps domain objects pure")
        void model_must_not_depend_on_controller() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(MODEL)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
                    .because("Model (entities) must not depend on any higher layer — " +
                             "keeps domain objects pure");

            rule.check(classes);
        }

        @Test
        @DisplayName("Model must not depend on service layer")
        void model_must_not_depend_on_service() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(MODEL)
                    .should().dependOnClassesThat().resideInAPackage(SERVICE)
                    .because("Model must not depend on service layer");

            rule.check(classes);
        }

        @Test
        @DisplayName("Model must not depend on repository — avoids circular JPA references")
        void model_must_not_depend_on_repository() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(MODEL)
                    .should().dependOnClassesThat().resideInAPackage(REPOSITORY)
                    .because("Model must not depend on repository to avoid circular JPA references");

            rule.check(classes);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DTO constraints
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DTO layer constraints")
    class DtoConstraints {

        @Test
        @DisplayName("[ARCH-DRIFT-07] DTOs must not depend on service — plain data carriers only")
        void dto_must_not_depend_on_service() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(DTO)
                    .should().dependOnClassesThat().resideInAPackage(SERVICE)
                    .because("DTOs are plain data carriers — must not depend on service " +
                             "or controller (ARCH-DRIFT-07)");

            rule.check(classes);
        }

        @Test
        @DisplayName("DTOs must not depend on controller layer")
        void dto_must_not_depend_on_controller() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(DTO)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
                    .because("DTOs must not depend on controller layer");

            rule.check(classes);
        }

        @Test
        @DisplayName("DTOs must not depend on repository — no data access from transport objects")
        void dto_must_not_depend_on_repository() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(DTO)
                    .should().dependOnClassesThat().resideInAPackage(REPOSITORY)
                    .because("DTOs must not depend on repository — " +
                             "no data access from transport objects");

            rule.check(classes);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPER constraints
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Mapper layer constraints")
    class MapperConstraints {

        @Test
        @DisplayName("[ARCH-DRIFT-08] Mappers must not depend on service — translates model↔dto only")
        void mapper_must_not_depend_on_service() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(MAPPER)
                    .should().dependOnClassesThat().resideInAPackage(SERVICE)
                    .because("Mappers translate between model and dto only — " +
                             "must not call service logic (ARCH-DRIFT-08)");

            rule.check(classes);
        }

        @Test
        @DisplayName("Mappers must not depend on controller")
        void mapper_must_not_depend_on_controller() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(MAPPER)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
                    .because("Mappers must not depend on controller");

            rule.check(classes);
        }

        @Test
        @DisplayName("[ARCH-DRIFT-09] Mappers must not depend on repository — no direct data access")
        void mapper_must_not_depend_on_repository() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(MAPPER)
                    .should().dependOnClassesThat().resideInAPackage(REPOSITORY)
                    .because("Mappers must not call repositories directly (ARCH-DRIFT-09)");

            rule.check(classes);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONTROLLER constraints
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Controller layer constraints")
    class ControllerConstraints {

        @Test
        @DisplayName("[ARCH-DRIFT-03] Controller must not depend on repository — bypasses business logic")
        void controller_must_not_depend_on_repository() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(CONTROLLER)
                    .should().dependOnClassesThat().resideInAPackage(REPOSITORY)
                    .because("Controller may only call service — direct repository access " +
                             "from controller bypasses business logic (ARCH-DRIFT-03)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[ARCH-DRIFT-04/05] Controller must not depend on model entities — use DTOs for transport")
        void controller_must_not_depend_on_model() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(CONTROLLER)
                    .should().dependOnClassesThat().resideInAPackage(MODEL)
                    .because("Controller must not import model entities directly — " +
                             "use DTOs for transport (ARCH-DRIFT-04, ARCH-DRIFT-05)");

            rule.check(classes);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIG constraints
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Config layer constraints")
    class ConfigConstraints {

        @Test
        @DisplayName("[ARCH-DRIFT-10] Config must not depend on controller — avoids circular config")
        void config_must_not_depend_on_controller() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(CONFIG)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
                    .because("Config may wire service beans but must not depend on controller — " +
                             "avoids circular config (ARCH-DRIFT-10)");

            rule.check(classes);
        }
    }
}
