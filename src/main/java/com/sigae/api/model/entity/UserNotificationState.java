package com.sigae.api.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.Instant;

@Entity
@Table(name = "user_notification_state")
public class UserNotificationState extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "notification_id", nullable = false)
  private AppNotification notification;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column
  private Instant readAt;

  protected UserNotificationState() {}

  public UserNotificationState(AppNotification notification, User user) {
    this.notification = notification;
    this.user = user;
  }

  public AppNotification getNotification() {
    return notification;
  }

  public User getUser() {
    return user;
  }

  public Instant getReadAt() {
    return readAt;
  }

  public void markAsRead(Instant readAt) {
    this.readAt = readAt;
  }
}
