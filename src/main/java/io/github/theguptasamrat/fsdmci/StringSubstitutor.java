package io.github.theguptasamrat.fsdmci;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringSubstitutor {
  private static final Pattern TOKEN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");

  private StringSubstitutor() {
  }

  public static String replace(String value, Map<String, String> variables) {
    if (value == null || value.isEmpty()) {
      return value == null ? "" : value;
    }
    Matcher matcher = TOKEN.matcher(value);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      String replacement = variables.getOrDefault(matcher.group(1), matcher.group(0));
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
