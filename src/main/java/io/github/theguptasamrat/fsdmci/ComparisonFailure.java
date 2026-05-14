package io.github.theguptasamrat.fsdmci;

public record ComparisonFailure(
    String stageName,
    String entityName,
    String tableName,
    String keyValue,
    String reason,
    String expectedJson,
    String actualJson
) {
}
