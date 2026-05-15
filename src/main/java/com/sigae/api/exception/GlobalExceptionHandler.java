package com.sigae.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException exception,
      HttpServletRequest request
  ) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
    }

    return ResponseEntity.badRequest()
        .body(ApiError.validation(
            "La solicitud contiene datos inválidos.",
            request.getRequestURI(),
            fieldErrors
        ));
  }

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<ApiError> handleNotFound(NotFoundException exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiError.of(404, "Not Found", exception.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(ConflictException.class)
  ResponseEntity<ApiError> handleConflict(ConflictException exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiError.of(409, "Conflict", exception.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(BadRequestException.class)
  ResponseEntity<ApiError> handleBadRequest(BadRequestException exception, HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(ApiError.of(400, "Bad Request", exception.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(MailDeliveryException.class)
  ResponseEntity<ApiError> handleMailDelivery(
      MailDeliveryException exception,
      HttpServletRequest request
  ) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ApiError.of(
            503,
            "Service Unavailable",
            exception.getMessage(),
            request.getRequestURI()
        ));
  }

  @ExceptionHandler(BadCredentialsException.class)
  ResponseEntity<ApiError> handleBadCredentials(
      BadCredentialsException exception,
      HttpServletRequest request
  ) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiError.of(401, "Unauthorized", exception.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  ResponseEntity<ApiError> handleAccessDenied(
      AuthorizationDeniedException exception,
      HttpServletRequest request
  ) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiError.of(
            403,
            "Forbidden",
            "No tiene permisos para realizar esta acción.",
            request.getRequestURI()
        ));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> handleUnhandled(Exception exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.of(
            500,
            "Internal Server Error",
            "Ocurrió un error inesperado.",
            request.getRequestURI()
        ));
  }
}
