package com.sigae.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final AuthenticationEntryPoint authenticationEntryPoint;

  public JwtAuthenticationFilter(
      JwtService jwtService,
      AuthenticationEntryPoint authenticationEntryPoint
  ) {
    this.jwtService = jwtService;
    this.authenticationEntryPoint = authenticationEntryPoint;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      AuthenticatedUser authenticatedUser = jwtService.parseAccessToken(header.substring(7));
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              authenticatedUser,
              null,
              List.of(new SimpleGrantedAuthority("ROLE_" + authenticatedUser.role().name()))
          );
      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(request, response);
    } catch (RuntimeException exception) {
      SecurityContextHolder.clearContext();
      authenticationEntryPoint.commence(request, response, new JwtAuthenticationException(exception.getMessage()));
    }
  }
}
