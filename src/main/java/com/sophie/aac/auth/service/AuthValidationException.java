package com.sophie.aac.auth.service;

/**
 * Domain-specific validation error for authentication and account workflows.
 * Extends IllegalArgumentException to preserve existing API error mapping behavior.
 */
public class AuthValidationException extends IllegalArgumentException {

  public AuthValidationException(String message) {
    super(message);
  }
}
