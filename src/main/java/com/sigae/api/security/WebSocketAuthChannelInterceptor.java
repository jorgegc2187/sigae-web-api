package com.sigae.api.security;

import java.security.Principal;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
  private static final String SESSION_AUTHENTICATION_KEY = "websocket.authentication";

  private final JwtService jwtService;

  public WebSocketAuthChannelInterceptor(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
    StompCommand command = accessor.getCommand();
    if (command == null) {
      return message;
    }

    return switch (command) {
      case CONNECT -> authenticate(accessor, message);
      case SUBSCRIBE -> authorizeSubscription(accessor, message);
      default -> message;
    };
  }

  private Message<?> authenticate(StompHeaderAccessor accessor, Message<?> message) {
    String header = firstHeader(accessor, "Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      throw new BadCredentialsException("Debe autenticarse para abrir el canal de notificaciones.");
    }

    AuthenticatedUser authenticatedUser = jwtService.parseAccessToken(header.substring(7));
    Authentication authentication = new UsernamePasswordAuthenticationToken(
        authenticatedUser,
        null,
        List.of(new SimpleGrantedAuthority("ROLE_" + authenticatedUser.role().name()))
    );
    accessor.setUser(authentication);
    if (accessor.getSessionAttributes() != null) {
      accessor.getSessionAttributes().put(SESSION_AUTHENTICATION_KEY, authentication);
    }
    return message;
  }

  private Message<?> authorizeSubscription(StompHeaderAccessor accessor, Message<?> message) {
    Principal principal = accessor.getUser();
    if (principal == null && accessor.getSessionAttributes() != null) {
      Object storedAuthentication = accessor.getSessionAttributes().get(SESSION_AUTHENTICATION_KEY);
      if (storedAuthentication instanceof Authentication authentication) {
        accessor.setUser(authentication);
        principal = authentication;
      }
    }

    if (!(principal instanceof Authentication authentication)
        || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
      throw new BadCredentialsException("Debe autenticarse para suscribirse a notificaciones.");
    }

    String destination = accessor.getDestination();
    if ("/topic/notifications/admin".equals(destination) && authenticatedUser.role() != com.sigae.api.model.entity.UserRole.ADMINISTRADOR) {
      throw new AccessDeniedException("No tiene permisos para suscribirse a este canal.");
    }

    return message;
  }

  private String firstHeader(StompHeaderAccessor accessor, String headerName) {
    List<String> values = accessor.getNativeHeader(headerName);
    if (values != null && !values.isEmpty()) {
      return values.getFirst();
    }

    values = accessor.getNativeHeader(headerName.toLowerCase());
    return values == null || values.isEmpty() ? null : values.getFirst();
  }
}
