package io.github.theguptasamrat.fsdmci;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RfcJobClient {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final PipelineConfig config;
  private final HttpClient httpClient;

  public RfcJobClient(PipelineConfig config) {
    this.config = config;
    long timeoutSeconds = Long.parseLong(config.variables().getOrDefault("RFC_TIMEOUT_SECONDS", "300"));
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(timeoutSeconds))
        .build();
  }

  public void call(JobConfig job) throws IOException, InterruptedException {
    if (config.rfcHttpBaseUrl().isBlank()) {
      throw new IllegalStateException("RFC_HTTP_BASE_URL is required for job execution.");
    }

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("runId", config.runId());
    payload.put("jobName", job.jobName());
    payload.put("parameters", job.parameters());

    String body = OBJECT_MAPPER.writeValueAsString(payload);
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .uri(URI.create(joinUrl(config.rfcHttpBaseUrl(), job.endpointPath())))
        .timeout(Duration.ofSeconds(Long.parseLong(config.variables().getOrDefault("RFC_TIMEOUT_SECONDS", "300"))))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body));

    if (!config.rfcHttpToken().isBlank()) {
      requestBuilder.header("Authorization", "Bearer " + config.rfcHttpToken());
    }

    HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IllegalStateException("RFC job " + job.jobName() + " failed with HTTP "
          + response.statusCode() + ": " + response.body());
    }

    System.out.printf("RFC job %s completed with HTTP %d.%n", job.jobName(), response.statusCode());
  }

  private static String joinUrl(String baseUrl, String path) {
    if (baseUrl.endsWith("/") && path.startsWith("/")) {
      return baseUrl.substring(0, baseUrl.length() - 1) + path;
    }
    if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
      return baseUrl + "/" + path;
    }
    return baseUrl + path;
  }
}
