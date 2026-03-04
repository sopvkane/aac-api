package com.sophie.aac.preferences.controller;

import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.util.AuthContext;
import com.sophie.aac.common.web.ForbiddenException;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import com.sophie.aac.preferences.domain.PreferenceItemKind;
import com.sophie.aac.preferences.service.PreferenceItemService;
import com.sophie.aac.preferences.web.PreferenceItemRequest;
import com.sophie.aac.preferences.web.PreferenceItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/carer/preferences")
public class PreferenceItemController {

    private static final Set<PreferenceItemKind> CARER_KINDS = Set.of(
        PreferenceItemKind.FOOD, PreferenceItemKind.DRINK, PreferenceItemKind.ACTIVITY
    );
    private static final Set<PreferenceItemKind> SCHOOL_KINDS = Set.of(
        PreferenceItemKind.TEACHER, PreferenceItemKind.BUS_STAFF, PreferenceItemKind.SCHOOL_PEER,
        PreferenceItemKind.SUBJECT, PreferenceItemKind.SCHOOL_ACTIVITY
    );

    private final PreferenceItemService service;
    private final AuthContext authContext;

    public PreferenceItemController(PreferenceItemService service, AuthContext authContext) {
        this.service = service;
        this.authContext = authContext;
    }

    private static boolean canAccessKind(Role role, String kind) {
        if (role == null) return false;
        if (role == Role.PARENT || role == Role.CLINICIAN) return true;
        var parsedKind = PreferenceItemKind.from(kind);
        if (parsedKind.isEmpty()) return false;
        if (role == Role.CARER && CARER_KINDS.contains(parsedKind.get())) return true;
        if ((role == Role.SCHOOL_ADMIN || role == Role.SCHOOL_TEACHER) && SCHOOL_KINDS.contains(parsedKind.get())) return true;
        return false;
    }

    @GetMapping
    public List<PreferenceItemResponse> list(@RequestParam String kind) {
        if (!canAccessKind(authContext.currentRole(), kind)) {
            throw new ForbiddenException("Access denied for this preference type");
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
        Role role = authContext.currentRole();
        if (!canAccessKind(role, req.kind())) {
            throw new ForbiddenException("Access denied for this preference type");
        }
        PreferenceItemEntity e = service.create(req, role != null ? role.name() : "PARENT");
        return toResponse(e);
    }

    @PutMapping("/{id}")
    public PreferenceItemResponse update(@PathVariable UUID id, @RequestBody @Valid PreferenceItemRequest req) {
        if (!canAccessKind(authContext.currentRole(), req.kind())) {
            throw new ForbiddenException("Access denied for this preference type");
        }
        PreferenceItemEntity e = service.update(id, req);
        return toResponse(e);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        PreferenceItemEntity existing = service.findById(id);
        if (existing != null && !canAccessKind(authContext.currentRole(), existing.getKind())) {
            throw new ForbiddenException("Access denied for this preference type");
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
