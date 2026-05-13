package com.sigae.api.support;

import tools.jackson.databind.ObjectMapper;
import com.sigae.api.repository.PasswordResetRequestRepository;
import com.sigae.api.repository.RefreshTokenRepository;
import com.sigae.api.model.entity.User;
import com.sigae.api.repository.UserRepository;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected UserRepository userRepository;

  @Autowired
  protected RefreshTokenRepository refreshTokenRepository;

  @Autowired
  protected PasswordResetRequestRepository passwordResetRequestRepository;

  @Autowired
  protected PasswordEncoder passwordEncoder;

  @BeforeEach
  void cleanDatabase() {
    refreshTokenRepository.deleteAll();
    passwordResetRequestRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected User createUser(String fullName, String email, String password, UserRole role, UserStatus status) {
    User user = new User(fullName, email, passwordEncoder.encode(password), role, status);
    return userRepository.save(user);
  }

  protected String loginAndGetAccessToken(String email, String password) throws Exception {
    String response = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    return objectMapper.readTree(response).get("accessToken").asText();
  }
}
