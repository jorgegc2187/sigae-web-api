package com.sigae.api.config;

import com.sigae.api.security.WebSocketAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final SecurityProperties securityProperties;
  private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;

  public WebSocketConfig(
      SecurityProperties securityProperties,
      WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor
  ) {
    this.securityProperties = securityProperties;
    this.webSocketAuthChannelInterceptor = webSocketAuthChannelInterceptor;
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .setAllowedOrigins(securityProperties.cors().allowedOrigins().toArray(String[]::new));
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic");
    registry.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(webSocketAuthChannelInterceptor);
  }
}
