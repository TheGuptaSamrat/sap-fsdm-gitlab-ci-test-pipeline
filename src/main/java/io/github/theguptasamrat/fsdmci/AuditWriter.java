package io.github.theguptasamrat.fsdmci;

import java.sql.SQLException;
import java.util.List;

public final class AuditWriter {
  private final PipelineConfig config;
  private final DbClient dbClient;

  public AuditWriter(PipelineConfig config, DbClient dbClient) {
    this.config = config;
    this.dbClient = dbClient;
  }

  public void writeFailures(List<ComparisonFailure> failures) {
    if (failures.isEmpty()) {
      return;
    }
    try {
      dbClient.writeAuditRows(config.auditTable(), config.runId(), failures);
      dbClient.commit();
      System.out.printf("Wrote %d validation failure(s) to audit table %s.%n", failures.size(), config.auditTable());
    } catch (SQLException exception) {
      dbClient.rollbackQuietly();
      System.err.printf("Could not write validation failures to audit table %s: %s%n", config.auditTable(), exception.getMessage());
    }
  }
}
