package com.loanapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.BeforeAllCallback;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture constraint tests derived from architecture.json.
 *
 * Violations appear in SonarCloud as:
 *   1. Failed tests (Tests panel)  — via Surefire XML
 *   2. Issues (Issues panel)       — via target/archunit-sonar-report.json
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

    private static final String CONTROLLER = "com.loanapp.controller..";
    private static final String SERVICE     = "com.loanapp.service..";
    private static final String REPOSITORY  = "com.loanapp.repository..";
    private static final String MODEL       = "com.loanapp.model..";
    private static final String DTO         = "com.loanapp.dto..";
    private static final String MAPPER      = "com.loanapp.mapper..";
    private static final String CONFIG      = "com.loanapp.config..";

    private static JavaClasses classes;

    // Collects all violations across all nested test classes for Sonar report
    static final List<SonarIssue> sonarIssues = new ArrayList<>();
    static final List<SonarRule>  sonarRules  = new ArrayList<>();

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @AfterAll
    static void writeSonarReport() throws Exception {
        File dir = new File("target");
        dir.mkdirs();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("rules", sonarRules);
        report.put("issues", sonarIssues);

        new ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValue(new File("target/archunit-sonar-report.json"), report);
    }

    /**
     * Evaluates an ArchRule, registers it as a Sonar rule+issue if violated,
     * then re-throws so JUnit still marks the test as FAILED.
     */
    static void checkAndReport(String ruleId, String ruleName, String description,
                                String filePath, ArchRule rule) {
        // Register the rule definition once
        boolean ruleKnown = sonarRules.stream().anyMatch(r -> r.id.equals(ruleId));
        if (!ruleKnown) {
            SonarRule sr = new SonarRule();
            sr.id          = ruleId;
            sr.name        = ruleName;
            sr.description = description;
            sr.engineId    = "ArchUnit";
            sr.cleanCodeAttribute = "CONVENTIONAL";
            sr.impacts     = List.of(Map.of("softwareQuality", "MAINTAINABILITY", "severity", "HIGH"));
            sonarRules.add(sr);
        }

        EvaluationResult result = rule.evaluate(classes);
        if (result.hasViolation()) {
            // One Sonar issue per violation detail line
            result.getFailureReport().getDetails().forEach(detail -> {
                SonarIssue issue = new SonarIssue();
                issue.ruleId       = ruleId;
                issue.effortMinutes = 30;
                SonarLocation loc  = new SonarLocation();
                loc.message        = detail;
                loc.filePath       = filePath;
                issue.primaryLocation = loc;
                sonarIssues.add(issue);
            });

            // Re-throw so JUnit marks the test FAILED → Surefire XML captures it
            rule.check(classes);
        }
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
            checkAndReport(
                "ARCH-DRIFT-01",
                "Service must not depend on controller",
                "Service layer must never call back into controller — prevents reverse dependency cycle",
                "src/main/java/com/loanapp/service/CustomerService.java",
                noClasses().that().resideInAPackage(SERVICE)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
                    .because("Service layer must never call back into controller — " +
                             "prevents reverse dependency cycle (ARCH-DRIFT-01, ARCH-DRIFT-02)")
            );
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
            checkAndReport(
                "ARCH-DRIFT-06",
                "Repository must not depend on service",
                "Repository must never call service — data layer must remain unaware of business logic",
                "src/main/java/com/loanapp/repository",
                noClasses().that().resideInAPackage(REPOSITORY)
                    .should().dependOnClassesThat().resideInAPackage(SERVICE)
                    .because("Repository must never call service — data layer must remain " +
                             "unaware of business logic (ARCH-DRIFT-06)")
            );
        }

        @Test
        @DisplayName("Repository must never depend on controller — no upward dependency from data layer")
        void repository_must_not_depend_on_controller() {
            checkAndReport(
                "ARCH-DRIFT-06b",
                "Repository must not depend on controller",
                "Repository must never call controller — no upward dependency from data layer",
                "src/main/java/com/loanapp/repository",
                noClasses().that().resideInAPackage(REPOSITORY)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
                    .because("Repository must never call controller — " +
                             "no upward dependency from data layer")
            );
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
            checkAndReport(
                "ARCH-MODEL-01",
                "Model must not depend on controller",
                "Model (entities) must not depend on any higher layer — keeps domain objects pure",
                "src/main/java/com/loanapp/model",
                noClasses().that().resideInAPackage(MODEL)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
                    .because("Model (entities) must not depend on any higher layer — " +
                             "keeps domain objects pure")
            );
        }

        @Test
        @DisplayName("Model must not depend on service layer")
        void model_must_not_depend_on_service() {
            checkAndReport(
                "ARCH-MODEL-02",
                "Model must not depend on service",
                "Model must not depend on service layer",
                "src/main/java/com/loanapp/model",
                noClasses().that().resideInAPackage(MODEL)
                    .should().dependOnClassesThat().resideInAPackage(SERVICE)
                    .because("Model must not depend on service layer")
            );
        }

        @Test
        @DisplayName("Model must not depend on repository — avoids circular JPA references")
        void model_must_not_depend_on_repository() {
            checkAndReport(
                "ARCH-MODEL-03",
                "Model must not depend on repository",
                "Model must not depend on repository to avoid circular JPA references",
                "src/main/java/com/loanapp/model",
                noClasses().that().resideInAPackage(MODEL)
                    .should().dependOnClassesThat().resideInAPackage(REPOSITORY)
                    .because("Model must not depend on repository to avoid circular JPA references")
            );
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
            checkAndReport(
                "ARCH-DRIFT-07",
                "DTO must not depend on service",
                "DTOs are plain data carriers — must not depend on service or controller",
                "src/main/java/com/loanapp/dto/LoanRequest.java",
                noClasses().that().resideInAPackage(DTO)
                    .should().dependOnClassesThat().resideInAPackage(SERVICE)
                    .because("DTOs are plain data carriers — must not depend on service " +
                             "or controller (ARCH-DRIFT-07)")
            );
        }

        @Test
        @DisplayName("DTOs must not depend on controller layer")
        void dto_must_not_depend_on_controller() {
            checkAndReport(
                "ARCH-DTO-02",
                "DTO must not depend on controller",
                "DTOs must not depend on controller layer",
                "src/main/java/com/loanapp/dto",
                noClasses().that().resideInAPackage(DTO)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
                    .because("DTOs must not depend on controller layer")
            );
        }

        @Test
        @DisplayName("DTOs must not depend on repository — no data access from transport objects")
        void dto_must_not_depend_on_repository() {
            checkAndReport(
                "ARCH-DTO-03",
                "DTO must not depend on repository",
                "DTOs must not depend on repository — no data access from transport objects",
                "src/main/java/com/loanapp/dto",
                noClasses().that().resideInAPackage(DTO)
                    .should().dependOnClassesThat().resideInAPackage(REPOSITORY)
                    .because("DTOs must not depend on repository — " +
                             "no data access from transport objects")
            );
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
            checkAndReport(
                "ARCH-DRIFT-08",
                "Mapper must not depend on service",
                "Mappers translate between model and dto only — must not call service logic",
                "src/main/java/com/loanapp/mapper",
                noClasses().that().resideInAPackage(MAPPER)
                    .should().dependOnClassesThat().resideInAPackage(SERVICE)
                    .because("Mappers translate between model and dto only — " +
                             "must not call service logic (ARCH-DRIFT-08)")
            );
        }

        @Test
        @DisplayName("Mappers must not depend on controller")
        void mapper_must_not_depend_on_controller() {
            checkAndReport(
                "ARCH-MAPPER-02",
                "Mapper must not depend on controller",
                "Mappers must not depend on controller",
                "src/main/java/com/loanapp/mapper",
                noClasses().that().resideInAPackage(MAPPER)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
                    .because("Mappers must not depend on controller")
            );
        }

        @Test
        @DisplayName("[ARCH-DRIFT-09] Mappers must not depend on repository — no direct data access")
        void mapper_must_not_depend_on_repository() {
            checkAndReport(
                "ARCH-DRIFT-09",
                "Mapper must not depend on repository",
                "Mappers must not call repositories directly",
                "src/main/java/com/loanapp/mapper",
                noClasses().that().resideInAPackage(MAPPER)
                    .should().dependOnClassesThat().resideInAPackage(REPOSITORY)
                    .because("Mappers must not call repositories directly (ARCH-DRIFT-09)")
            );
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
            checkAndReport(
                "ARCH-DRIFT-03",
                "Controller must not depend on repository",
                "Controller may only call service — direct repository access from controller bypasses business logic",
                "src/main/java/com/loanapp/controller/CustomerController.java",
                noClasses().that().resideInAPackage(CONTROLLER)
                    .should().dependOnClassesThat().resideInAPackage(REPOSITORY)
                    .because("Controller may only call service — direct repository access " +
                             "from controller bypasses business logic (ARCH-DRIFT-03)")
            );
        }

        @Test
        @DisplayName("[ARCH-DRIFT-04/05] Controller must not depend on model entities — use DTOs for transport")
        void controller_must_not_depend_on_model() {
            checkAndReport(
                "ARCH-DRIFT-04",
                "Controller must not depend on model",
                "Controller must not import model entities directly — use DTOs for transport",
                "src/main/java/com/loanapp/controller/LoanController.java",
                noClasses().that().resideInAPackage(CONTROLLER)
                    .should().dependOnClassesThat().resideInAPackage(MODEL)
                    .because("Controller must not import model entities directly — " +
                             "use DTOs for transport (ARCH-DRIFT-04, ARCH-DRIFT-05)")
            );
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
            checkAndReport(
                "ARCH-DRIFT-10",
                "Config must not depend on controller",
                "Config may wire service beans but must not depend on controller — avoids circular config",
                "src/main/java/com/loanapp/config/SecurityConfig.java",
                noClasses().that().resideInAPackage(CONFIG)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
                    .because("Config may wire service beans but must not depend on controller — " +
                             "avoids circular config (ARCH-DRIFT-10)")
            );
        }
    }

    // ─── JSON model classes ────────────────────────────────────────────────────

    static class SonarRule {
        public String id;
        public String name;
        public String description;
        public String engineId;
        public String cleanCodeAttribute;
        public List<Map<String, String>> impacts;
    }

    static class SonarIssue {
        public String ruleId;
        public int effortMinutes;
        public SonarLocation primaryLocation;
    }

    static class SonarLocation {
        public String message;
        public String filePath;
    }
}