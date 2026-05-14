package io.github.theguptasamrat.fsdmci;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CsvTable(List<String> headers, List<Map<String, String>> rows) {
  public static CsvTable read(Path path, Map<String, String> variables) throws IOException {
    CSVFormat format = CSVFormat.DEFAULT.builder()
        .setHeader()
        .setSkipHeaderRecord(true)
        .setTrim(true)
        .build();

    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
         CSVParser parser = format.parse(reader)) {
      List<String> headers = List.copyOf(parser.getHeaderNames());
      List<Map<String, String>> rows = new ArrayList<>();
      for (CSVRecord record : parser) {
        Map<String, String> row = new LinkedHashMap<>();
        for (String header : headers) {
          String value = record.isMapped(header) && record.isSet(header) ? record.get(header) : "";
          row.put(header, StringSubstitutor.replace(value, variables));
        }
        rows.add(row);
      }
      return new CsvTable(headers, rows);
    }
  }
}
