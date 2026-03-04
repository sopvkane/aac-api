package com.sophie.aac.preferences.controller;

import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.util.CurrentRole;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import com.sophie.aac.preferences.service.PreferenceItemService;
import com.sophie.aac.preferences.web.PreferenceItemRequest;
import com.sophie.aac.preferences.web.PreferenceItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/carer/preferences")
public class PreferenceItemController {

    private static final Set<String> CARER_KINDS = Set.of("FOOD", "DRINK", "ACTIVITY");
    private static final Set<String> SCHOOL_KINDS = Set.of("TEACHER", "BUS_STAFF", "SCHOOL_PEER", "SUBJECT", "SCHOOL_ACTIVITY");

    private final PreferenceItemService service;

    public PreferenceItemController(PreferenceItemService service) {
        this.service = service;
    }

    private static boolean canAccessKind(Role role, String kind) {
        if (role == null) return false;
        String k = kind == null ? "" : kind.toUpperCase();
        if (role == Role.PARENT || role == Role.CLINICIAN) return true;
        if (role == Role.CARER && CARER_KINDS.contains(k)) return true;
        if ((role == Role.SCHOOL_ADMIN || role == Role.SCHOOL_TEACHER) && SCHOOL_KINDS.contains(k)) return true;
        return false;
    }

    @GetMapping
    public List<PreferenceItemResponse> list(@RequestParam String kind) {
        if (!canAccessKind(CurrentRole.get(), kind)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for this preference type");
        }
        return service.listByKind(kind).stream().map(PreferenceItemController::toResponse).toList();
    }

    /**
     * Returns "who to ask" people for the Speak tab, filtered by location.
     * Use location=SCHOOL for teachers/bus staff, location=HOME for family.
     * Response header X-Suggested-Icon-Size: large – use large icons for face images.
     */
    @GetMapping("/who-to-ask")
    public ResponseEntity<List<PreferenceItemResponse>> whoToAsk(@RequestParam(defaultValue = "HOME") String location) {
        var people = service.listWhoToAskByLocation(location).stream().map(PreferenceItemController::toResponse).toList();
        return ResponseEntity.ok()
            .header("X-Suggested-Icon-Size", "large")
            .body(people);
    }

    @PostMapping
    public PreferenceItemResponse create(@RequestBody @Valid PreferenceItemRequest req) {
        Role role = CurrentRole.get();
        if (!canAccessKind(role, req.kind())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for this preference type");
        }
        PreferenceItemEntity e = service.create(req, role != null ? role.name() : "PARENT");
        return toResponse(e);
    }

    @PutMapping("/{id}")
    public PreferenceItemResponse update(@PathVariable UUID id, @RequestBody @Valid PreferenceItemRequest req) {
        if (!canAccessKind(CurrentRole.get(), req.kind())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for this preference type");
        }
        PreferenceItemEntity e = service.update(id, req);
        return toResponse(e);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        PreferenceItemEntity existing = service.findById(id);
        if (existing != null && !canAccessKind(CurrentRole.get(), existing.getKind())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for this preference type");
        }
        service.delete(id);
    }

    private static PreferenceItemResponse toResponse(PreferenceItemEntity e) {
        return new PreferenceItemResponse(
            e.getId(),
            e.getKind(),
            e.getLabel(),
            e.getCategory(),
            e.getTags(),
            e.getImageUrl(),
            e.getScope(),
            e.getPriority()
        );
    }
}

