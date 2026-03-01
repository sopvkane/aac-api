package com.sophie.aac.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class TokenHash {
  private TokenHash() {}

  public static String sha256Hex(String token) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] out = md.digest(token.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : out) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to hash token", e);
    }
  }
}