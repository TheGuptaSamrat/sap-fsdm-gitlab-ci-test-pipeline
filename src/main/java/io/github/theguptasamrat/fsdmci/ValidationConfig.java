package io.github.theguptasamrat.fsdmci;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ValidationConfig(
    String stageName,
    String entityName,
    String tableName,
    String expectedCsv,
    List<String> keyColumns,
    List<String> compareColumns,
    String whereClause
) {
  public static List<ValidationConfig> load(Path path, Map<String, String> variables) throws IOException {
    CsvTable table = CsvTable.read(path, variables);
    List<ValidationConfig> configs = new ArrayList<>();
    for (Map<String, String> row : table.rows()) {
      configs.add(new ValidationConfig(
          EntityConfig.required(row, "stage_name"),
          EntityConfig.required(row, "entity_name"),
          EntityConfig.required(row, "table_name"),
          EntityConfig.required(row, "expected_csv"),
          EntityConfig.parseColumns(EntityConfig.required(row, "key_columns")),
          EntityConfig.parseColumns(row.getOrDefault("compare_columns", "")),
          row.getOrDefault("where_clause", "")
      ));
    }
    return List.copyOf(configs);
  }
}
