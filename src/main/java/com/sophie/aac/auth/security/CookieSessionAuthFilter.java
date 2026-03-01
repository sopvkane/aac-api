package com.sophie.aac.auth.security;

import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.repository.AuthSessionRepository;
import com.sophie.aac.auth.service.AuthService;
import com.sophie.aac.auth.util.TokenHash;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class CookieSessionAuthFilter extends OncePerRequestFilter {

  private final AuthSessionRepository sessions;

  public CookieSessionAuthFilter(AuthSessionRepository sessions) {
    this.sessions = sessions;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws java.io.IOException, jakarta.servlet.ServletException {
    String token = readCookie(request, AuthService.COOKIE_NAME);

    if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      String hash = TokenHash.sha256Hex(token);

      sessions.findByTokenHash(hash).ifPresent(session -> {
        if (session.getExpiresAt().isAfter(Instant.now())) {
          Role role = session.getRole();
          var auth = new UsernamePasswordAuthenticationToken(
              "session:" + session.getId(),
              null,
              List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
          );
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      });
    }

    filterChain.doFilter(request, response);
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