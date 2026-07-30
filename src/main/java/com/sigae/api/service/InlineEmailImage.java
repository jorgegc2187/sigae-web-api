package com.sigae.api.service;

public record InlineEmailImage(
    String contentId,
    String fileName,
    String contentType,
    byte[] content
) {}
