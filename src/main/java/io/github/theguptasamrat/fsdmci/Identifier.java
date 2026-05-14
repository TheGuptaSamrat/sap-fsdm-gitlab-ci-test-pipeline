package io.github.theguptasamrat.fsdmci;

public final class Identifier {
  private Identifier() {
  }

  public static String table(String schema, String tableName) {
    if (schema == null || schema.isBlank()) {
      return quote(tableName);
    }
    return quote(schema) + "." + quote(tableName);
  }

  public static String column(String columnName) {
    return quote(columnName);
  }

  private static String quote(String identifier) {
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }
}
