package io.github.theguptasamrat.fsdmci;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record EntityConfig(
    String entityName,
    String tableName,
    String inputCsv,
    List<String> keyColumns,
    List<String> compareColumns,
    InsertMode insertMode,
    String whereClause
) {
  public enum InsertMode {
    APPEND,
    DELETE_INSERT
  }

  public static List<EntityConfig> load(Path path, Map<String, String> variables) throws IOException {
    CsvTable table = CsvTable.read(path, variables);
    List<EntityConfig> configs = new ArrayList<>();
    for (Map<String, String> row : table.rows()) {
      String entityName = required(row, "entity_name");
      String tableName = required(row, "table_name");
      String inputCsv = required(row, "input_csv");
      List<String> keyColumns = parseColumns(required(row, "key_columns"));
      List<String> compareColumns = parseColumns(row.getOrDefault("compare_columns", ""));
      String rawMode = row.getOrDefault("insert_mode", "").trim();
      String mode = (rawMode.isEmpty() ? "APPEND" : rawMode).toUpperCase(Locale.ROOT);
      String whereClause = row.getOrDefault("where_clause", "");
      configs.add(new EntityConfig(entityName, tableName, inputCsv, keyColumns, compareColumns, InsertMode.valueOf(mode), whereClause));
    }
    return List.copyOf(configs);
  }

  public static List<String> parseColumns(String value) {
    List<String> columns = new ArrayList<>();
    for (String raw : value.split("\\|")) {
      String column = raw.trim();
      if (!column.isEmpty()) {
        columns.add(column);
      }
    }
    return List.copyOf(columns);
  }

  static String required(Map<String, String> row, String column) {
    String value = row.getOrDefault(column, "").trim();
    if (value.isEmpty()) {
      throw new IllegalArgumentException("Missing required config column: " + column + " in row " + row);
    }
    return value;
  }
}
