package io.github.theguptasamrat.fsdmci;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringSubstitutorTest {
  @Test
  void replacesKnownVariablesAndLeavesUnknownTokensIntact() {
    String resolved = StringSubstitutor.replace("RUN=${RUN_ID};X=${UNKNOWN}", Map.of("RUN_ID", "42"));

    assertEquals("RUN=42;X=${UNKNOWN}", resolved);
  }
}
