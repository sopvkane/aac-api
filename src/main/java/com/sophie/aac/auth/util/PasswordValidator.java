package com.sophie.aac.auth.util;

import java.util.regex.Pattern;

/** Validates password meets complexity: length, uppercase, lowercase, digit, symbol. */
public final class PasswordValidator {

  private static final int MIN_LENGTH = 10;
  private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
  private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
  private static final Pattern DIGIT = Pattern.compile("[0-9]");
  private static final Pattern SYMBOL = Pattern.compile("[^A-Za-z0-9]");

  private PasswordValidator() {}

  public static void validate(String password) {
    if (password == null || password.length() < MIN_LENGTH) {
      throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters");
    }
    if (!UPPERCASE.matcher(password).find()) {
      throw new IllegalArgumentException("Password must contain an uppercase letter");
    }
    if (!LOWERCASE.matcher(password).find()) {
      throw new IllegalArgumentException("Password must contain a lowercase letter");
    }
    if (!DIGIT.matcher(password).find()) {
      throw new IllegalArgumentException("Password must contain a number");
    }
    if (!SYMBOL.matcher(password).find()) {
      throw new IllegalArgumentException("Password must contain a symbol");
    }
  }
}
