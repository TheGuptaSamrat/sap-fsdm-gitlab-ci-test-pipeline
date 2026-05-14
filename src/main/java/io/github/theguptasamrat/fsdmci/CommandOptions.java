package io.github.theguptasamrat.fsdmci;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public record CommandOptions(
    String command,
    Path entityConfigPath,
    Path jobConfigPath,
    Path validationConfigPath,
    Path inputDir,
    Path expectedDir,
    boolean dryRun
) {
  public static CommandOptions parse(String[] args) {
    String command = args.length > 0 && !args[0].startsWith("--") ? args[0] : "validate-config";
    int start = command.equals("validate-config") && (args.length == 0 || args[0].startsWith("--")) ? 0 : 1;

    Map<String, String> values = new LinkedHashMap<>();
    boolean dryRun = false;
    for (int index = start; index < args.length; index++) {
      String arg = args[index];
      if ("--dry-run".equals(arg)) {
        dryRun = true;
        continue;
      }
      if (!arg.startsWith("--")) {
        throw new IllegalArgumentException("Unexpected argument: " + arg);
      }
      if (index + 1 >= args.length) {
        throw new IllegalArgumentException("Missing value for argument: " + arg);
      }
      values.put(arg.substring(2), args[++index]);
    }

    return new CommandOptions(
        command,
        Path.of(values.getOrDefault("entities", "config/entities.csv")),
        Path.of(values.getOrDefault("jobs", "config/jobs.csv")),
        Path.of(values.getOrDefault("validations", "config/validations.csv")),
        Path.of(values.getOrDefault("input-dir", "data/input")),
        Path.of(values.getOrDefault("expected-dir", "data/expected")),
        dryRun
    );
  }
}
