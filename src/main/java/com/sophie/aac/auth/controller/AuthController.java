package com.sophie.aac.auth.controller;

import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.service.AuthService;
import com.sophie.aac.auth.util.AuthContext;
import com.sophie.aac.common.web.BadRequestException;
import com.sophie.aac.common.web.UnauthorizedException;
import com.sophie.aac.profile.repository.UserProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Authentication and multi-tenant profile management")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService auth;
  private final UserProfileRepository profileRepo;
  private final AuthContext authContext;

  public AuthController(AuthService auth, UserProfileRepository profileRepo, AuthContext authContext) {
    this.auth = auth;
    this.profileRepo = profileRepo;
    this.authContext = authContext;
  }

  public record ProfileSummary(java.util.UUID id, String displayName) {}
  public record LoginRequest(String email, String password, String pin, String role) {}
  public record RegisterRequest(@NotBlank String displayName, @NotBlank String email, @NotBlank String password,
      @NotBlank String role, @NotBlank String joiningCode) {}
  public record MeResponse(Role role, java.util.UUID activeProfileId, java.util.List<java.util.UUID> profileIds, java.util.List<ProfileSummary> profiles) {}
  public record LoginResponse(Role role, java.util.UUID activeProfileId, java.util.List<java.util.UUID> profileIds, java.util.List<ProfileSummary> profiles) {}
  public record SelectProfileRequest(@NotNull java.util.UUID profileId) {}

  @Operation(summary = "Login", description = "Authenticate with email+password (full account) or PIN (delegated). Sets AAC_SESSION cookie.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Success"),
      @ApiResponse(responseCode = "400", description = "Invalid credentials")
  })
  @PostMapping("/login")
  public LoginResponse login(@RequestBody @Valid LoginRequest req, HttpServletResponse res) {
    AuthService.LoginResult result;
    if (req.email() != null && !req.email().isBlank() && req.password() != null) {
      result = auth.loginWithPassword(req.email().trim(), req.password());
    } else if (req.role() != null && !req.role().isBlank() && req.pin() != null && !req.pin().isBlank()) {
      result = auth.login(parseRole(req.role()), req.pin());
    } else if (req.pin() != null && !req.pin().isBlank()) {
      result = auth.loginWithPin(req.pin());
    } else {
      throw new BadRequestException("Provide email+password, role+pin, or delegated pin");
    }

    ResponseCookie cookie = ResponseCookie.from(AuthService.COOKIE_NAME, result.token())
        .httpOnly(true)
        .sameSite("Lax")
        .path("/")
        .maxAge(result.ttlMinutes() * 60)
        .secure(false)
        .build();

    res.addHeader("Set-Cookie", cookie.toString());
    var profiles = toProfileSummaries(profileRepo.findAllById(result.profileIds()));
    return new LoginResponse(result.role(), result.activeProfileId(), result.profileIds(), profiles);
  }

  @Operation(summary = "Register", description = "Create account with joining code from clinician. Role: PARENT_CARER or CLINICIAN.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Registered"),
      @ApiResponse(responseCode = "400", description = "Invalid input or code")
  })
  @PostMapping("/register")
  public LoginResponse register(@RequestBody @Valid RegisterRequest req, HttpServletResponse res) {
    auth.register(req.displayName(), req.email(), req.password(), req.role(), req.joiningCode());
    AuthService.LoginResult result = auth.loginWithPassword(req.email().trim(), req.password());

    ResponseCookie cookie = ResponseCookie.from(AuthService.COOKIE_NAME, result.token())
        .httpOnly(true)
        .sameSite("Lax")
        .path("/")
        .maxAge(result.ttlMinutes() * 60)
        .secure(false)
        .build();

    res.addHeader("Set-Cookie", cookie.toString());
    var profiles = toProfileSummaries(profileRepo.findAllById(result.profileIds()));
    return new LoginResponse(result.role(), result.activeProfileId(), result.profileIds(), profiles);
  }

  @Operation(summary = "Select profile", description = "Switch active communicator/profile. Requires AAC_SESSION cookie. User must have access to the profile (profileId in profileIds from login/me).")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Profile switched"),
      @ApiResponse(responseCode = "401", description = "Not logged in or invalid session"),
      @ApiResponse(responseCode = "403", description = "No access to requested profile")
  })
  @PostMapping("/select-profile")
  public MeResponse selectProfile(@RequestBody @Valid SelectProfileRequest req, HttpServletRequest servletReq) {
    String token = readCookie(servletReq, AuthService.COOKIE_NAME);
    auth.selectProfile(token, req.profileId());

    org.springframework.security.core.Authentication authn =
        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    if (authn == null || !authn.isAuthenticated()) {
      throw new UnauthorizedException("Not logged in");
    }
    String roleStr = authn.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    Role role = Role.valueOf(roleStr);

    var newAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
        authn.getPrincipal(),
        authn.getCredentials(),
        authn.getAuthorities()
    );
    newAuth.setDetails(java.util.Map.of("profileId", req.profileId()));
    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(newAuth);

    java.util.List<java.util.UUID> profileIds = auth.getProfileIdsForSession(token);
    var profiles = toProfileSummaries(profileRepo.findAllById(profileIds));
    return new MeResponse(role, req.profileId(), profileIds, profiles);
  }

  @Operation(summary = "Logout", description = "Clears AAC_SESSION cookie.")
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

  @Operation(summary = "Get current user", description = "Returns role, activeProfileId, profileIds, and profiles. Requires AAC_SESSION cookie.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Success"),
      @ApiResponse(responseCode = "401", description = "Not logged in")
  })
  @GetMapping("/me")
  public MeResponse me(org.springframework.security.core.Authentication authn, HttpServletRequest req) {
    if (authn == null || !authn.isAuthenticated()) {
      throw new UnauthorizedException("Not logged in");
    }
    String roleStr = authn.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    Role role = Role.valueOf(roleStr);
    java.util.UUID activeProfileId = authContext.currentProfileId();
    String token = readCookie(req, AuthService.COOKIE_NAME);
    java.util.List<java.util.UUID> profileIds = auth.getProfileIdsForSession(token);
    var profiles = toProfileSummaries(profileRepo.findAllById(profileIds));
    return new MeResponse(role, activeProfileId, profileIds, profiles);
  }

  private static java.util.List<ProfileSummary> toProfileSummaries(java.util.List<com.sophie.aac.profile.domain.UserProfileEntity> entities) {
    return entities.stream()
        .map(p -> new ProfileSummary(p.getId(), p.getDisplayName()))
        .toList();
  }

  private static String readCookie(HttpServletRequest req, String name) {
    Cookie[] cookies = req.getCookies();
    if (cookies == null) return null;
    for (Cookie c : cookies) {
      if (name.equals(c.getName())) return c.getValue();
    }
    return null;
  }

  private static Role parseRole(String roleRaw) {
    String normalized = roleRaw.trim().replace('-', '_').replace(' ', '_').toUpperCase();
    if ("TEACHER".equals(normalized)) normalized = "SCHOOL_TEACHER";
    try {
      return Role.valueOf(normalized);
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException("Invalid role");
    }
  }
}
