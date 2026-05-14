package io.github.theguptasamrat.fsdmci;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public final class TableLoader {
  private final PipelineConfig config;
  private final DbClient dbClient;
  private final AuditWriter auditWriter;

  public TableLoader(PipelineConfig config, DbClient dbClient) {
    this.config = config;
    this.dbClient = dbClient;
    this.auditWriter = new AuditWriter(config, dbClient);
  }

  public void loadAllOrThrow() throws IOException, SQLException {
    List<EntityConfig> entities = EntityConfig.load(config.entityConfigPath(), config.variables());
    for (EntityConfig entity : entities) {
      ComparisonResult result = loadAndValidate(entity);
      auditWriter.writeFailures(result.failures());
      if (!result.passed()) {
        throw new IllegalStateException("Load validation failed for " + entity.entityName() + " with "
            + result.failureCount() + " mismatch(es).");
      }
    }
  }

  public ComparisonResult loadAndValidate(EntityConfig entity) throws IOException, SQLException {
    CsvTable inputTable = CsvTable.read(config.inputDir().resolve(entity.inputCsv()), config.variables());
    List<String> compareColumns = entity.compareColumns().isEmpty() ? inputTable.headers() : entity.compareColumns();
    List<String> selectColumns = DbClient.unionColumns(entity.keyColumns(), compareColumns);

    try {
      if (entity.insertMode() == EntityConfig.InsertMode.DELETE_INSERT) {
        dbClient.deleteAll(entity.tableName());
      }
      dbClient.insertRows(entity.tableName(), inputTable.rows());
      dbClient.commit();
    } catch (SQLException exception) {
      dbClient.rollbackQuietly();
      throw exception;
    }

    List<Map<String, String>> actualRows = readActualRows(entity.tableName(), selectColumns, entity.keyColumns(), entity.whereClause(), inputTable.rows());
    ComparisonResult result = DataComparator.compare("load", entity.entityName(), entity.tableName(), inputTable.rows(), actualRows, entity.keyColumns(), compareColumns);

    if (result.passed()) {
      System.out.printf("Loaded and validated %d row(s) for entity %s into %s.%n", inputTable.rows().size(), entity.entityName(), entity.tableName());
    }
    return result;
  }

  private List<Map<String, String>> readActualRows(
      String tableName,
      List<String> selectColumns,
      List<String> keyColumns,
      String whereClause,
      List<Map<String, String>> expectedRows
  ) throws SQLException {
    if (whereClause != null && !whereClause.isBlank()) {
      return dbClient.fetchRows(tableName, selectColumns, whereClause);
    }
    return dbClient.fetchRowsByKeys(tableName, selectColumns, keyColumns, expectedRows);
  }
}
