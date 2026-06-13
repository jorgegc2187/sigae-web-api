package com.sigae.api.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;

public final class DotenvPropertiesLoader {
  private static final String SPRING_PROFILES_ACTIVE = "SPRING_PROFILES_ACTIVE";
  private static final String SPRING_PROFILES_ACTIVE_PROPERTY = "spring.profiles.active";

  private DotenvPropertiesLoader() {}

  public static Map<String, Object> loadDefaultProperties(Path dotenvPath) {
    return loadDefaultProperties(dotenvPath, System.getenv(), System.getProperties());
  }

  public static boolean shouldLoadForDevProfile(String[] args) {
    return shouldLoadForDevProfile(args, System.getenv(), System.getProperties());
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

  static boolean shouldLoadForDevProfile(
      String[] args,
      Map<String, String> environmentVariables,
      Map<?, ?> systemProperties
  ) {
    String activeProfiles = firstNonBlank(
        extractActiveProfiles(args),
        Objects.toString(systemProperties.get(SPRING_PROFILES_ACTIVE_PROPERTY), null),
        environmentVariables.get(SPRING_PROFILES_ACTIVE)
    );

    if (activeProfiles == null) {
      return false;
    }

    return Arrays.stream(activeProfiles.split(","))
        .map(String::trim)
        .filter(profile -> !profile.isEmpty())
        .map(profile -> profile.toLowerCase(Locale.ROOT))
        .anyMatch("dev"::equals);
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

  private static String extractActiveProfiles(String[] args) {
    if (args == null) {
      return null;
    }

    for (String rawArg : args) {
      if (rawArg == null) {
        continue;
      }

      String arg = rawArg.trim();
      if (arg.startsWith("--" + SPRING_PROFILES_ACTIVE_PROPERTY + "=")) {
        return arg.substring(("--" + SPRING_PROFILES_ACTIVE_PROPERTY + "=").length()).trim();
      }
      if (arg.startsWith("-D" + SPRING_PROFILES_ACTIVE_PROPERTY + "=")) {
        return arg.substring(("-D" + SPRING_PROFILES_ACTIVE_PROPERTY + "=").length()).trim();
      }
    }

    return null;
  }

  private static String firstNonBlank(String... candidates) {
    for (String candidate : candidates) {
      if (candidate != null && !candidate.isBlank()) {
        return candidate;
      }
    }

    return null;
  }
}
