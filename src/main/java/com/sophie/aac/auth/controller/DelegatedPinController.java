package com.sophie.aac.auth.controller;

import com.sophie.aac.auth.domain.DelegatedPinEntity;
import com.sophie.aac.auth.service.DelegatedPinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Authentication and profile management")
@RestController
@RequestMapping("/api/auth/pins")
public class DelegatedPinController {

  private final DelegatedPinService delegatedPinService;

  public DelegatedPinController(DelegatedPinService delegatedPinService) {
    this.delegatedPinService = delegatedPinService;
  }

  public record CreatePinRequest(@NotBlank String pin, String label, @NotNull UUID profileId) {}
  public record PinSummary(UUID id, String label, UUID profileId, Instant createdAt, boolean active) {}

  @Operation(summary = "Create delegated PIN", description = "Parent/clinician creates a PIN for shared device access. Requires full account login.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "PIN created"),
      @ApiResponse(responseCode = "400", description = "Invalid input"),
      @ApiResponse(responseCode = "401", description = "Not logged in"),
      @ApiResponse(responseCode = "403", description = "No access to profile")
  })
  @PostMapping
  public PinSummary create(@RequestBody @Valid CreatePinRequest req) {
    DelegatedPinEntity created = delegatedPinService.create(req.label(), req.pin(), req.profileId());
    return toSummary(created);
  }

  @Operation(summary = "List delegated PINs", description = "List PINs created by the current user.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Success"),
      @ApiResponse(responseCode = "401", description = "Not logged in")
  })
  @GetMapping
  public List<PinSummary> list() {
    return delegatedPinService.listByCurrentUser().stream()
        .map(this::toSummary)
        .toList();
  }

  @Operation(summary = "Revoke delegated PIN", description = "Deactivate a PIN. Only the creator can revoke.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Revoked"),
      @ApiResponse(responseCode = "401", description = "Not logged in"),
      @ApiResponse(responseCode = "403", description = "Not creator"),
      @ApiResponse(responseCode = "404", description = "PIN not found")
  })
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revoke(@PathVariable UUID id) {
    delegatedPinService.revoke(id);
  }

  private PinSummary toSummary(DelegatedPinEntity e) {
    return new PinSummary(e.getId(), e.getLabel(), e.getProfileId(), e.getCreatedAt(), e.isActive());
  }
}
