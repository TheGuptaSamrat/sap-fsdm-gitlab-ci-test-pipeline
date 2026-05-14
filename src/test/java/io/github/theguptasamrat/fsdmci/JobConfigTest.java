package io.github.theguptasamrat.fsdmci;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobConfigTest {
  @Test
  void loadsJobsInSequenceOrderWithMandatoryPp01() throws Exception {
    Path file = Files.createTempFile("jobs", ".csv");
    Files.writeString(file, """
        sequence,stage_name,job_name,endpoint_path,PP_01,PP_02
        20,z30,Z30,/rfc/jobs,run-1,chain
        10,activate,Activate,/rfc/jobs,run-1,
        """);

    List<JobConfig> jobs = JobConfig.load(file, Map.of());

    assertEquals("activate", jobs.get(0).stageName());
    assertEquals("z30", jobs.get(1).stageName());
  }

  @Test
  void rejectsJobWithoutPp01() throws Exception {
    Path file = Files.createTempFile("jobs", ".csv");
    Files.writeString(file, """
        sequence,stage_name,job_name,endpoint_path,PP_01
        10,activate,Activate,/rfc/jobs,
        """);

    assertThrows(IllegalArgumentException.class, () -> JobConfig.load(file, Map.of()));
  }
}
