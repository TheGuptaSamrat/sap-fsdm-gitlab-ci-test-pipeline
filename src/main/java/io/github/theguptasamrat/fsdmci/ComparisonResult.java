package io.github.theguptasamrat.fsdmci;

import java.util.List;

public record ComparisonResult(List<ComparisonFailure> failures) {
  public boolean passed() {
    return failures.isEmpty();
  }

  public int failureCount() {
    return failures.size();
  }
}
