package com.sigae.api.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DotenvPropertiesLoader {

  private DotenvPropertiesLoader() {}

  public static Map<String, Object> loadDefaultProperties(Path dotenvPath) {
    return loadDefaultProperties(dotenvPath, System.getenv(), System.getProperties());
  }

  static Map<String, Object> loadDefaultProperties(
      Path dotenvPath,
      Map<String, String> environmentVariables,
      Map<?, ?> systemProperties
  ) {
    if (dotenvPath == null || !Files.isRegularFile(dotenvPath)) {
      return Map.of();
    }

    LinkedHashMap<String, Object> properties = new LinkedHashMap<>();

    try {
      List<String> lines = Files.readAllLines(dotenvPath, StandardCharsets.UTF_8);
      for (String rawLine : lines) {
        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }

        int separatorIndex = line.indexOf('=');
        if (separatorIndex <= 0) {
          continue;
        }

        String key = line.substring(0, separatorIndex).trim();
        if (key.isEmpty()
            || environmentVariables.containsKey(key)
            || systemProperties.containsKey(key)) {
          continue;
        }

        String value = line.substring(separatorIndex + 1).trim();
        properties.put(key, stripWrappingQuotes(value));
      }
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo leer el archivo .env de desarrollo.", exception);
    }

    return properties;
  }

  private static String stripWrappingQuotes(String value) {
    if (value.length() >= 2) {
      if ((value.startsWith("\"") && value.endsWith("\""))
          || (value.startsWith("'") && value.endsWith("'"))) {
        return value.substring(1, value.length() - 1);
      }
    }

    return value;
  }
}
