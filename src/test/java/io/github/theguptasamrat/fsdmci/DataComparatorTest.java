package io.github.theguptasamrat.fsdmci;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataComparatorTest {
  @Test
  void passesWhenRowsMatchByConfiguredKeyAndColumns() {
    List<Map<String, String>> expected = List.of(Map.of(
        "RUN_ID", "123",
        "CONTRACT_ID", "FC-1",
        "AMOUNT", "100.00"
    ));
    List<Map<String, String>> actual = List.of(Map.of(
        "RUN_ID", "123",
        "CONTRACT_ID", "FC-1",
        "AMOUNT", "100.00"
    ));

    ComparisonResult result = DataComparator.compare(
        "load",
        "financial_contract",
        "/FSDM/D_FIN_CONTRACT",
        expected,
        actual,
        List.of("RUN_ID", "CONTRACT_ID"),
        List.of("AMOUNT")
    );

    assertTrue(result.passed());
  }

  @Test
  void recordsMismatchReasonWhenConfiguredColumnDiffers() {
    List<Map<String, String>> expected = List.of(Map.of(
        "RUN_ID", "123",
        "CONTRACT_ID", "FC-1",
        "AMOUNT", "100.00"
    ));
    List<Map<String, String>> actual = List.of(Map.of(
        "RUN_ID", "123",
        "CONTRACT_ID", "FC-1",
        "AMOUNT", "99.00"
    ));

    ComparisonResult result = DataComparator.compare(
        "activate",
        "financial_contract",
        "/FSDM/A_FIN_CONTRACT",
        expected,
        actual,
        List.of("RUN_ID", "CONTRACT_ID"),
        List.of("AMOUNT")
    );

    assertEquals(1, result.failureCount());
    assertTrue(result.failures().get(0).reason().contains("AMOUNT"));
  }
}
