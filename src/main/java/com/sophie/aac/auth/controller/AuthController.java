package com.sophie.aac.auth.controller;

import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  public record LoginRequest(@NotNull Role role, @NotBlank String pin) {}
  public record MeResponse(Role role) {}
  public record LoginResponse(Role role) {}

  @PostMapping("/login")
  public LoginResponse login(@RequestBody @Valid LoginRequest req, HttpServletResponse res) {
    var result = auth.login(req.role(), req.pin());

    ResponseCookie cookie = ResponseCookie.from(AuthService.COOKIE_NAME, result.token())
        .httpOnly(true)
        .sameSite("Lax")
        .path("/")
        .maxAge(result.ttlMinutes() * 60)
        .secure(false) // set true if you serve over https
        .build();

    res.addHeader("Set-Cookie", cookie.toString());
    return new LoginResponse(result.role());
  }

  @PostMapping("/logout")
  public void logout(HttpServletRequest req, HttpServletResponse res) {
    String token = readCookie(req, AuthService.COOKIE_NAME);
    auth.logout(token);

    ResponseCookie cleared = ResponseCookie.from(AuthService.COOKIE_NAME, "")
        .httpOnly(true)
        .sameSite("Lax")
        .path("/")
        .maxAge(0)
        .secure(false)
        .build();

    res.addHeader("Set-Cookie", cleared.toString());
  }

  @GetMapping("/me")
  public MeResponse me(org.springframework.security.core.Authentication authn) {
    if (authn == null || !authn.isAuthenticated()) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNAUTHORIZED, "Not logged in");
    }
    // role stored as ROLE_PARENT etc
    String role = authn.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    return new MeResponse(Role.valueOf(role));
  }

  private static String readCookie(HttpServletRequest req, String name) {
    Cookie[] cookies = req.getCookies();
    if (cookies == null) return null;
    for (Cookie c : cookies) {
      if (name.equals(c.getName())) return c.getValue();
    }
    return null;
  }
}