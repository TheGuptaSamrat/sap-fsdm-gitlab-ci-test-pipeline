package io.github.theguptasamrat.fsdmci;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;

public final class DbClient implements AutoCloseable {
  private final Connection connection;
  private final String schema;

  private DbClient(Connection connection, String schema) {
    this.connection = connection;
    this.schema = schema;
  }

  public static DbClient connect(PipelineConfig config) throws Exception {
    Class.forName(config.hanaDriverClass());
    Properties properties = new Properties();
    properties.put("user", config.hanaUser());
    properties.put("password", config.hanaPassword());
    Connection connection = DriverManager.getConnection(config.hanaJdbcUrl(), properties);
    connection.setAutoCommit(false);
    return new DbClient(connection, config.hanaSchema());
  }

  public void deleteAll(String tableName) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate("DELETE FROM " + Identifier.table(schema, tableName));
    }
  }

  public void insertRows(String tableName, List<Map<String, String>> rows) throws SQLException {
    if (rows.isEmpty()) {
      return;
    }

    List<String> columns = new ArrayList<>(rows.get(0).keySet());
    StringJoiner columnList = new StringJoiner(", ");
    StringJoiner placeholders = new StringJoiner(", ");
    for (String column : columns) {
      columnList.add(Identifier.column(column));
      placeholders.add("?");
    }

    String sql = "INSERT INTO " + Identifier.table(schema, tableName)
        + " (" + columnList + ") VALUES (" + placeholders + ")";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (Map<String, String> row : rows) {
        for (int index = 0; index < columns.size(); index++) {
          statement.setString(index + 1, row.getOrDefault(columns.get(index), ""));
        }
        statement.addBatch();
      }
      statement.executeBatch();
    }
  }

  public List<Map<String, String>> fetchRows(String tableName, List<String> columns, String whereClause) throws SQLException {
    String sql = "SELECT " + selectList(columns)
        + " FROM " + Identifier.table(schema, tableName)
        + (whereClause == null || whereClause.isBlank() ? "" : " WHERE " + whereClause);

    try (Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery(sql)) {
      return readRows(resultSet, columns);
    }
  }

  public List<Map<String, String>> fetchRowsByKeys(
      String tableName,
      List<String> columns,
      List<String> keyColumns,
      List<Map<String, String>> expectedRows
  ) throws SQLException {
    if (expectedRows.isEmpty()) {
      return List.of();
    }

    StringJoiner predicates = new StringJoiner(" OR ");
    for (int rowIndex = 0; rowIndex < expectedRows.size(); rowIndex++) {
      StringJoiner keyPredicate = new StringJoiner(" AND ");
      for (String keyColumn : keyColumns) {
        keyPredicate.add(Identifier.column(keyColumn) + " = ?");
      }
      predicates.add("(" + keyPredicate + ")");
    }

    String sql = "SELECT " + selectList(columns)
        + " FROM " + Identifier.table(schema, tableName)
        + " WHERE " + predicates;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int parameterIndex = 1;
      for (Map<String, String> row : expectedRows) {
        for (String keyColumn : keyColumns) {
          statement.setString(parameterIndex++, row.getOrDefault(keyColumn, ""));
        }
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        return readRows(resultSet, columns);
      }
    }
  }

  public void writeAuditRows(String auditTable, String runId, List<ComparisonFailure> failures) throws SQLException {
    if (auditTable == null || auditTable.isBlank() || failures.isEmpty()) {
      return;
    }

    String sql = "INSERT INTO " + Identifier.table(schema, auditTable)
        + " (\"RUN_ID\", \"STAGE_NAME\", \"ENTITY_NAME\", \"TABLE_NAME\", \"KEY_VALUE\", \"FAILURE_REASON\", \"EXPECTED_JSON\", \"ACTUAL_JSON\")"
        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (ComparisonFailure failure : failures) {
        statement.setString(1, runId);
        statement.setString(2, failure.stageName());
        statement.setString(3, failure.entityName());
        statement.setString(4, failure.tableName());
        statement.setString(5, failure.keyValue());
        statement.setString(6, failure.reason());
        statement.setString(7, failure.expectedJson());
        statement.setString(8, failure.actualJson());
        statement.addBatch();
      }
      statement.executeBatch();
    }
  }

  public void commit() throws SQLException {
    connection.commit();
  }

  public void rollbackQuietly() {
    try {
      connection.rollback();
    } catch (SQLException ignored) {
      // Best effort only. The original failure is more useful.
    }
  }

  @Override
  public void close() throws SQLException {
    connection.close();
  }

  static List<String> unionColumns(List<String> first, List<String> second) {
    Set<String> union = new LinkedHashSet<>();
    union.addAll(first);
    union.addAll(second);
    return List.copyOf(union);
  }

  private static String selectList(List<String> columns) {
    StringJoiner selectList = new StringJoiner(", ");
    for (String column : columns) {
      selectList.add(Identifier.column(column));
    }
    return selectList.toString();
  }

  private static List<Map<String, String>> readRows(ResultSet resultSet, List<String> columns) throws SQLException {
    List<Map<String, String>> rows = new ArrayList<>();
    while (resultSet.next()) {
      Map<String, String> row = new LinkedHashMap<>();
      for (String column : columns) {
        row.put(column, resultSet.getString(column));
      }
      rows.add(row);
    }
    return rows;
  }
}
