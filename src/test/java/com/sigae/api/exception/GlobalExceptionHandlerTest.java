package com.sigae.api.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new OversizedUploadTestController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void maxUploadSizeExceededReturnsApiErrorPayload() throws Exception {
    mockMvc.perform(get("/test/max-upload").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.status").value(413))
        .andExpect(jsonPath("$.error").value("Payload Too Large"))
        .andExpect(jsonPath("$.message").value("Los archivos adjuntos exceden el tamaño máximo permitido."))
        .andExpect(jsonPath("$.path").value("/test/max-upload"));
  }

  @RestController
  static class OversizedUploadTestController {

    @GetMapping("/test/max-upload")
    void failWithMaxUploadSizeExceeded() {
      throw new MaxUploadSizeExceededException(25L * 1024 * 1024);
    }
  }
}
