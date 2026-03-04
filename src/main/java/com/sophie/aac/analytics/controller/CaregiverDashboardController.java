package com.sophie.aac.analytics.controller;

import com.sophie.aac.analytics.service.CaregiverDashboardService;
import com.sophie.aac.analytics.web.CaregiverDashboardResponse;
import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.util.CurrentRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carer/dashboard")
public class CaregiverDashboardController {

    private final CaregiverDashboardService service;

    public CaregiverDashboardController(CaregiverDashboardService service) {
        this.service = service;
    }

    @GetMapping
    public CaregiverDashboardResponse get(@RequestParam(name = "period", defaultValue = "WEEK") String period) {
        Role role = CurrentRole.get();
        boolean includePain = role == Role.PARENT || role == Role.CLINICIAN;
        return service.getDashboard(period, includePain);
    }
}

