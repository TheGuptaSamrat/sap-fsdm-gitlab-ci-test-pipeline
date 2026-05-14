package io.github.theguptasamrat.fsdmci;

public final class App {
  private App() {
  }

  public static void main(String[] args) throws Exception {
    CommandOptions options = CommandOptions.parse(args);
    PipelineConfig config = PipelineConfig.from(options);

    switch (options.command()) {
      case "validate-config" -> {
        config.validateStaticConfiguration();
        System.out.printf("Configuration OK. Run id: %s%n", config.runId());
      }
      case "load-validate" -> {
        config.validateStaticConfiguration();
        if (config.dryRun()) {
          System.out.println("Dry run enabled; JDBC load and validation were not executed.");
          return;
        }
        try (DbClient dbClient = DbClient.connect(config)) {
          TableLoader tableLoader = new TableLoader(config, dbClient);
          tableLoader.loadAllOrThrow();
        }
      }
      case "run-flow" -> {
        config.validateStaticConfiguration();
        if (config.dryRun()) {
          FlowRunner.printDryRun(config);
          return;
        }
        new FlowRunner(config).run();
      }
      default -> throw new IllegalArgumentException("Unknown command: " + options.command());
    }
  }
}
