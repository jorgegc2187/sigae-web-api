package com.sigae.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DotenvPropertiesLoaderTest {

  @TempDir
  Path tempDir;

  @Test
  void loadsValuesFromDotenvWhenTheyAreNotAlreadyPresent() throws Exception {
    Path dotenv = tempDir.resolve(".env");
    Files.writeString(
        dotenv,
        """
        MAIL_HOST=smtp.gmail.com
        MAIL_PORT=587
        MAIL_PASSWORD="abc 123"
        # comentario
        APP_FRONTEND_URL=http://localhost:4200
        """,
        StandardCharsets.UTF_8
    );

    Map<String, Object> properties = DotenvPropertiesLoader.loadDefaultProperties(
        dotenv,
        Map.of(),
        System.getProperties()
    );

    assertThat(properties)
        .containsEntry("MAIL_HOST", "smtp.gmail.com")
        .containsEntry("MAIL_PORT", "587")
        .containsEntry("MAIL_PASSWORD", "abc 123")
        .containsEntry("APP_FRONTEND_URL", "http://localhost:4200");
  }

  @Test
  void keepsProcessEnvironmentAndSystemPropertiesPrecedenceOverDotenv() throws Exception {
    Path dotenv = tempDir.resolve(".env");
    Files.writeString(
        dotenv,
        """
        MAIL_HOST=smtp.gmail.com
        MAIL_USERNAME=from-dotenv@example.com
        """,
        StandardCharsets.UTF_8
    );

    Map<String, Object> properties = DotenvPropertiesLoader.loadDefaultProperties(
        dotenv,
        Map.of("MAIL_HOST", "smtp.outlook.com"),
        Map.of("MAIL_USERNAME", "from-system@example.com")
    );

    assertThat(properties)
        .doesNotContainKey("MAIL_HOST")
        .doesNotContainKey("MAIL_USERNAME");
  }
}
