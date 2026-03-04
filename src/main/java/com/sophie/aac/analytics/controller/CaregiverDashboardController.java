package com.sophie.aac.analytics.controller;

import com.sophie.aac.analytics.service.CaregiverDashboardService;
import com.sophie.aac.analytics.web.CaregiverDashboardResponse;
import com.sophie.aac.auth.util.AuthContext;
import com.sophie.aac.auth.domain.Role;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carer/dashboard")
public class CaregiverDashboardController {

    private final CaregiverDashboardService service;
    private final AuthContext authContext;

    public CaregiverDashboardController(CaregiverDashboardService service, AuthContext authContext) {
        this.service = service;
        this.authContext = authContext;
    }

    @GetMapping
    public CaregiverDashboardResponse get(@RequestParam(name = "period", defaultValue = "WEEK") String period) {
        Role role = authContext.currentRole();
        boolean includePain = role == Role.PARENT || role == Role.CLINICIAN;
        return service.getDashboard(period, includePain);
    }
}
