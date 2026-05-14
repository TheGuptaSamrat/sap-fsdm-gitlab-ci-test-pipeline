package io.github.theguptasamrat.fsdmci;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JobConfig(
    int sequence,
    String stageName,
    String jobName,
    String endpointPath,
    Map<String, String> parameters
) {
  public static List<JobConfig> load(Path path, Map<String, String> variables) throws IOException {
    CsvTable table = CsvTable.read(path, variables);
    List<JobConfig> configs = new ArrayList<>();
    for (Map<String, String> row : table.rows()) {
      int sequence = Integer.parseInt(EntityConfig.required(row, "sequence"));
      String stageName = EntityConfig.required(row, "stage_name");
      String jobName = EntityConfig.required(row, "job_name");
      String endpointPath = EntityConfig.required(row, "endpoint_path");
      Map<String, String> parameters = new LinkedHashMap<>();
      for (int index = 1; index <= 10; index++) {
        String parameterName = String.format("PP_%02d", index);
        String value = row.getOrDefault(parameterName, "").trim();
        if (index == 1 && value.isEmpty()) {
          throw new IllegalArgumentException("PP_01 is mandatory for job " + jobName);
        }
        if (!value.isEmpty()) {
          parameters.put(parameterName, value);
        }
      }
      configs.add(new JobConfig(sequence, stageName, jobName, endpointPath, Collections.unmodifiableMap(new LinkedHashMap<>(parameters))));
    }
    configs.sort(Comparator.comparingInt(JobConfig::sequence));
    return List.copyOf(configs);
  }
}
