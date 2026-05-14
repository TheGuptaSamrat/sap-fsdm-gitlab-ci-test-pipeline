package io.github.theguptasamrat.fsdmci;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public final class DataComparator {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private DataComparator() {
  }

  public static ComparisonResult compare(
      String stageName,
      String entityName,
      String tableName,
      List<Map<String, String>> expectedRows,
      List<Map<String, String>> actualRows,
      List<String> keyColumns,
      List<String> compareColumns
  ) {
    List<ComparisonFailure> failures = new ArrayList<>();
    Map<String, Map<String, String>> expectedByKey = indexRows(stageName, entityName, tableName, "expected", expectedRows, keyColumns, failures);
    Map<String, Map<String, String>> actualByKey = indexRows(stageName, entityName, tableName, "actual", actualRows, keyColumns, failures);

    for (Map.Entry<String, Map<String, String>> expectedEntry : expectedByKey.entrySet()) {
      String key = expectedEntry.getKey();
      Map<String, String> expected = expectedEntry.getValue();
      Map<String, String> actual = actualByKey.get(key);
      if (actual == null) {
        failures.add(failure(stageName, entityName, tableName, key, "Missing actual row", expected, Map.of()));
        continue;
      }
      for (String column : compareColumns) {
        String expectedValue = normalize(expected.get(column));
        String actualValue = normalize(actual.get(column));
        if (!expectedValue.equals(actualValue)) {
          failures.add(failure(stageName, entityName, tableName, key,
              "Column " + column + " mismatch: expected [" + expectedValue + "] actual [" + actualValue + "]",
              expected,
              actual));
        }
      }
    }

    for (Map.Entry<String, Map<String, String>> actualEntry : actualByKey.entrySet()) {
      if (!expectedByKey.containsKey(actualEntry.getKey())) {
        failures.add(failure(stageName, entityName, tableName, actualEntry.getKey(), "Unexpected actual row", Map.of(), actualEntry.getValue()));
      }
    }

    return new ComparisonResult(List.copyOf(failures));
  }

  private static Map<String, Map<String, String>> indexRows(
      String stageName,
      String entityName,
      String tableName,
      String side,
      List<Map<String, String>> rows,
      List<String> keyColumns,
      List<ComparisonFailure> failures
  ) {
    Map<String, Map<String, String>> indexed = new LinkedHashMap<>();
    for (Map<String, String> row : rows) {
      String key = keyValue(row, keyColumns);
      if (indexed.containsKey(key)) {
        failures.add(failure(stageName, entityName, tableName, key, "Duplicate " + side + " row for key", indexed.get(key), row));
      }
      indexed.put(key, row);
    }
    return indexed;
  }

  private static String keyValue(Map<String, String> row, List<String> keyColumns) {
    StringJoiner joiner = new StringJoiner("|");
    for (String column : keyColumns) {
      joiner.add(column + "=" + normalize(row.get(column)));
    }
    return joiner.toString();
  }

  private static ComparisonFailure failure(
      String stageName,
      String entityName,
      String tableName,
      String keyValue,
      String reason,
      Map<String, String> expected,
      Map<String, String> actual
  ) {
    return new ComparisonFailure(stageName, entityName, tableName, keyValue, reason, toJson(expected), toJson(actual));
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim();
  }

  private static String toJson(Map<String, String> row) {
    try {
      return OBJECT_MAPPER.writeValueAsString(row);
    } catch (JsonProcessingException exception) {
      return "{}";
    }
  }
}
