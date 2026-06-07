package com.sigae.api.service;

import com.sigae.api.model.dto.LiveNotificationInvalidationEvent;
import java.time.Instant;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class LiveNotificationPublisher {

  private static final String GLOBAL_TOPIC = "/topic/notifications/global";
  private static final String ADMIN_TOPIC = "/topic/notifications/admin";

  private final SimpMessagingTemplate messagingTemplate;

  public LiveNotificationPublisher(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  public void publishGlobalInvalidation() {
    publishAfterCommit(GLOBAL_TOPIC, "global");
  }

  public void publishAdminInvalidation() {
    publishAfterCommit(ADMIN_TOPIC, "admin");
  }

  public void publishAllInvalidation() {
    publishGlobalInvalidation();
    publishAdminInvalidation();
  }

  @Scheduled(fixedDelay = 300_000L, initialDelay = 300_000L)
  public void publishPeriodicInvalidation() {
    publishAllInvalidation();
  }

  private void publishAfterCommit(String topic, String audience) {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          publish(topic, audience);
        }
      });
      return;
    }

    publish(topic, audience);
  }

  private void publish(String topic, String audience) {
    messagingTemplate.convertAndSend(topic, new LiveNotificationInvalidationEvent(audience, Instant.now()));
  }
}
