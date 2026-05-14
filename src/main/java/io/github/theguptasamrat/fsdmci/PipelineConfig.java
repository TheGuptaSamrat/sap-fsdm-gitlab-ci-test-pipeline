package io.github.theguptasamrat.fsdmci;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PipelineConfig(
    Path entityConfigPath,
    Path jobConfigPath,
    Path validationConfigPath,
    Path inputDir,
    Path expectedDir,
    Map<String, String> variables,
    boolean dryRun,
    String hanaJdbcUrl,
    String hanaUser,
    String hanaPassword,
    String hanaSchema,
    String hanaDriverClass,
    String rfcHttpBaseUrl,
    String rfcHttpToken,
    String auditTable
) {
  public static PipelineConfig from(CommandOptions options) {
    Map<String, String> variables = new LinkedHashMap<>(System.getenv());
    variables.putIfAbsent("RUN_ID", "LOCAL-" + DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now()));
    variables.putIfAbsent("BUSINESS_DATE", LocalDate.now().toString());

    boolean dryRun = options.dryRun() || Boolean.parseBoolean(variables.getOrDefault("DRY_RUN", "false"));

    return new PipelineConfig(
        options.entityConfigPath(),
        options.jobConfigPath(),
        options.validationConfigPath(),
        options.inputDir(),
        options.expectedDir(),
        Map.copyOf(variables),
        dryRun,
        variables.getOrDefault("HANA_JDBC_URL", ""),
        variables.getOrDefault("HANA_USER", ""),
        variables.getOrDefault("HANA_PASSWORD", ""),
        variables.getOrDefault("HANA_SCHEMA", ""),
        variables.getOrDefault("HANA_JDBC_DRIVER_CLASS", "com.sap.db.jdbc.Driver"),
        variables.getOrDefault("RFC_HTTP_BASE_URL", ""),
        variables.getOrDefault("RFC_HTTP_TOKEN", ""),
        variables.getOrDefault("AUDIT_TABLE", "ZCI_FSDM_FLOW_AUDIT")
    );
  }

  public String runId() {
    return variables.getOrDefault("RUN_ID", "");
  }

  public void validateStaticConfiguration() throws IOException {
    assertExists(entityConfigPath, "entity config");
    assertExists(jobConfigPath, "job config");
    assertExists(validationConfigPath, "validation config");
    assertExists(inputDir, "input data directory");
    assertExists(expectedDir, "expected data directory");

    List<EntityConfig> entities = EntityConfig.load(entityConfigPath, variables);
    for (EntityConfig entity : entities) {
      assertExists(inputDir.resolve(entity.inputCsv()), "input CSV for " + entity.entityName());
    }

    JobConfig.load(jobConfigPath, variables);

    List<ValidationConfig> validations = ValidationConfig.load(validationConfigPath, variables);
    for (ValidationConfig validation : validations) {
      assertExists(expectedDir.resolve(validation.expectedCsv()), "expected CSV for " + validation.stageName() + "/" + validation.entityName());
    }
  }

  private static void assertExists(Path path, String label) {
    if (!Files.exists(path)) {
      throw new IllegalArgumentException("Missing " + label + ": " + path);
    }
  }
}
