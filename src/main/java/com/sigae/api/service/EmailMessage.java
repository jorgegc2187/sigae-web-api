package com.sigae.api.service;

public record EmailMessage(
    String from,
    String to,
    String subject,
    String text,
    String html
) {}
