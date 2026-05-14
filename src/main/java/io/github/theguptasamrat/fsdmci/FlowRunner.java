package io.github.theguptasamrat.fsdmci;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public final class FlowRunner {
  private final PipelineConfig config;

  public FlowRunner(PipelineConfig config) {
    this.config = config;
  }

  public static void printDryRun(PipelineConfig config) throws IOException {
    List<EntityConfig> entities = EntityConfig.load(config.entityConfigPath(), config.variables());
    List<JobConfig> jobs = JobConfig.load(config.jobConfigPath(), config.variables());
    List<ValidationConfig> validations = ValidationConfig.load(config.validationConfigPath(), config.variables());
    System.out.printf("Dry run for run id %s.%n", config.runId());
    System.out.printf("Entities configured: %d%n", entities.size());
    System.out.printf("Jobs configured: %d%n", jobs.size());
    System.out.printf("Stage validations configured: %d%n", validations.size());
  }

  public void run() throws Exception {
    List<JobConfig> jobs = JobConfig.load(config.jobConfigPath(), config.variables());
    List<ValidationConfig> validations = ValidationConfig.load(config.validationConfigPath(), config.variables());

    try (DbClient dbClient = DbClient.connect(config)) {
      TableLoader tableLoader = new TableLoader(config, dbClient);
      tableLoader.loadAllOrThrow();

      RfcJobClient rfcJobClient = new RfcJobClient(config);
      AuditWriter auditWriter = new AuditWriter(config, dbClient);
      for (JobConfig job : jobs) {
        rfcJobClient.call(job);
        validateStage(dbClient, auditWriter, job.stageName(), validations);
      }
    }
  }

  private void validateStage(
      DbClient dbClient,
      AuditWriter auditWriter,
      String stageName,
      List<ValidationConfig> validations
  ) throws IOException, SQLException {
    int executed = 0;
    for (ValidationConfig validation : validations) {
      if (!stageName.equals(validation.stageName())) {
        continue;
      }
      executed++;
      CsvTable expectedTable = CsvTable.read(config.expectedDir().resolve(validation.expectedCsv()), config.variables());
      List<String> compareColumns = validation.compareColumns().isEmpty() ? expectedTable.headers() : validation.compareColumns();
      List<String> selectColumns = DbClient.unionColumns(validation.keyColumns(), compareColumns);
      List<Map<String, String>> actualRows;
      if (validation.whereClause() != null && !validation.whereClause().isBlank()) {
        actualRows = dbClient.fetchRows(validation.tableName(), selectColumns, validation.whereClause());
      } else {
        actualRows = dbClient.fetchRowsByKeys(validation.tableName(), selectColumns, validation.keyColumns(), expectedTable.rows());
      }

      ComparisonResult result = DataComparator.compare(
          stageName,
          validation.entityName(),
          validation.tableName(),
          expectedTable.rows(),
          actualRows,
          validation.keyColumns(),
          compareColumns
      );
      auditWriter.writeFailures(result.failures());
      if (!result.passed()) {
        throw new IllegalStateException("Validation failed for stage " + stageName + ", entity "
            + validation.entityName() + " with " + result.failureCount() + " mismatch(es).");
      }
      System.out.printf("Validated stage %s for entity %s against %s.%n", stageName, validation.entityName(), validation.expectedCsv());
    }

    if (executed == 0) {
      System.out.printf("No expected-data validation configured for stage %s.%n", stageName);
    }
  }
}
