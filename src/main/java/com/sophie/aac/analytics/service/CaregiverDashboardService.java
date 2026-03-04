package com.sophie.aac.analytics.service;

import com.sophie.aac.analytics.domain.InteractionEventEntity;
import com.sophie.aac.analytics.domain.WellbeingEntryEntity;
import com.sophie.aac.analytics.repository.InteractionEventRepository;
import com.sophie.aac.analytics.repository.WellbeingEntryRepository;
import com.sophie.aac.analytics.web.CaregiverDashboardResponse;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.service.CaregiverProfileService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CaregiverDashboardService {

    private final CaregiverProfileService profileService;
    private final InteractionEventRepository interactionEvents;
    private final WellbeingEntryRepository wellbeingEntries;

    public CaregiverDashboardService(
        CaregiverProfileService profileService,
        InteractionEventRepository interactionEvents,
        WellbeingEntryRepository wellbeingEntries
    ) {
        this.profileService = profileService;
        this.interactionEvents = interactionEvents;
        this.wellbeingEntries = wellbeingEntries;
    }

    /** Full dashboard. When includePain is false, pain fields are zeroed/empty for roles that cannot see pain data. */
    public CaregiverDashboardResponse getDashboard(String period, boolean includePain) {
        UserProfileEntity profile = profileService.get();
        UUID profileId = profile.getId();

        Instant now = Instant.now();
        ZoneId zone = ZoneId.systemDefault();

        Instant since = resolveSince(now, period);
        List<InteractionEventEntity> recentInteractions = interactionEvents.findByProfileIdAndCreatedAtAfter(profileId, since);
        List<WellbeingEntryEntity> recentWellbeing = wellbeingEntries.findByProfileIdAndCreatedAtAfter(profileId, since);

        DashboardMetricsCalculator.DashboardMetrics metrics = DashboardMetricsCalculator.calculate(
            recentInteractions, recentWellbeing, since, now, zone, includePain
        );

        return new CaregiverDashboardResponse(
            normalizePeriod(period),
            since,
            profile.getDisplayName(),
            profile.getFavFood(),
            profile.getFavDrink(),
            profile.getFavShow(),
            metrics.byBucket(),
            metrics.totalInteractions(),
            metrics.todayInteractions(),
            metrics.wellbeingEntries(),
            metrics.painEvents(),
            metrics.averagePainSeverity(),
            metrics.painEventsToday(),
            metrics.averagePainSeverityToday(),
            metrics.painByBodyArea(),
            metrics.moodDistribution(),
            metrics.moodPercent(),
            metrics.painPercent(),
            metrics.moodChart(),
            metrics.painChart(),
            metrics.painSeverityTimeSeries()
        );
    }

    private static Instant resolveSince(Instant now, String periodRaw) {
        String period = normalizePeriod(periodRaw);
        return switch (period) {
            case "DAY" -> now.minus(Duration.ofDays(1));
            case "WEEK" -> now.minus(Duration.ofDays(7));
            case "MONTH" -> now.minus(Duration.ofDays(30));
            case "3_MONTHS" -> now.minus(Duration.ofDays(90));
            case "6_MONTHS" -> now.minus(Duration.ofDays(180));
            case "9_MONTHS" -> now.minus(Duration.ofDays(270));
            case "YEAR" -> now.minus(Duration.ofDays(365));
            default -> now.minus(Duration.ofDays(7));
        };
    }

    private static String normalizePeriod(String raw) {
        if (raw == null) return "WEEK";
        String s = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (s) {
            case "1D", "TODAY", "DAY" -> "DAY";
            case "7D", "WEEK" -> "WEEK";
            case "30D", "MONTH" -> "MONTH";
            case "3M", "3MONTHS", "3_MONTHS" -> "3_MONTHS";
            case "6M", "6MONTHS", "6_MONTHS" -> "6_MONTHS";
            case "9M", "9MONTHS", "9_MONTHS" -> "9_MONTHS";
            case "12M", "365D", "YEAR" -> "YEAR";
            default -> s;
        };
    }
}
