package com.sigae.api.security;

import com.sigae.api.config.SecurityProperties;
import com.sigae.api.exception.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      AuthenticationEntryPoint authenticationEntryPoint,
      AccessDeniedHandler accessDeniedHandler,
      CorsConfigurationSource corsConfigurationSource
  ) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/actuator/health",
                "/api/auth/login",
                "/api/auth/refresh",
                "/api/auth/mfa/enroll/start",
                "/api/auth/mfa/enroll/confirm",
                "/api/auth/mfa/verify",
                "/api/auth/forgot-password",
                "/api/auth/reset-password",
                "/api/auth/reset-password/validate",
                "/api/settings/branding",
                "/api/settings/logo",
                "/ws",
                "/ws/**"
            ).permitAll()
            .anyRequest().authenticated())
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(SecurityProperties securityProperties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(securityProperties.cors().allowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    configuration.setExposedHeaders(List.of("Content-Disposition"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
    return (request, response, exception) ->
        writeError(response, objectMapper, HttpStatus.UNAUTHORIZED, "Unauthorized", resolveAuthMessage(exception), request.getRequestURI());
  }

  @Bean
  AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
    return (request, response, accessDeniedException) ->
        writeError(response, objectMapper, HttpStatus.FORBIDDEN, "Forbidden", "No tiene permisos para realizar esta acción.", request.getRequestURI());
  }

  private String resolveAuthMessage(AuthenticationException exception) {
    return exception instanceof JwtAuthenticationException
        ? exception.getMessage()
        : "Debe autenticarse para acceder a este recurso.";
  }

  private void writeError(
      HttpServletResponse response,
      ObjectMapper objectMapper,
      HttpStatus status,
      String error,
      String message,
      String path
  ) throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), ApiError.of(status.value(), error, message, path));
  }
}
