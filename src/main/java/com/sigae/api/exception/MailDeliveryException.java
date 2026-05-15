package com.sigae.api.exception;

public class MailDeliveryException extends RuntimeException {

  public MailDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
