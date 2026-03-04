package com.sophie.aac.analytics.service;

import com.sophie.aac.analytics.domain.InteractionEventEntity;
import com.sophie.aac.analytics.domain.WellbeingEntryEntity;
import com.sophie.aac.analytics.repository.InteractionEventRepository;
import com.sophie.aac.analytics.repository.WellbeingEntryRepository;
import com.sophie.aac.analytics.web.CaregiverDashboardResponse;
import com.sophie.aac.analytics.web.PainSeverityDataPoint;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.service.CaregiverProfileService;
import com.sophie.aac.suggestions.domain.TimeBucket;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.Locale;

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
        LocalDate today = LocalDate.now(zone);
        Instant todayStart = today.atStartOfDay(zone).toInstant();

        List<InteractionEventEntity> recentInteractions = interactionEvents.findByProfileIdAndCreatedAtAfter(profileId, since);
        List<WellbeingEntryEntity> recentWellbeing = wellbeingEntries.findByProfileIdAndCreatedAtAfter(profileId, since);

        Map<TimeBucket, Long> byBucket = new EnumMap<>(TimeBucket.class);
        for (TimeBucket b : TimeBucket.values()) {
            byBucket.put(b, 0L);
        }

        long todayInteractions = 0L;
        for (InteractionEventEntity e : recentInteractions) {
            TimeBucket bucket = bucketForInstant(e.getCreatedAt(), zone);
            byBucket.put(bucket, byBucket.get(bucket) + 1);

            if (!e.getCreatedAt().isBefore(todayStart)) {
                todayInteractions++;
            }
        }

        long wellbeingEntriesLast7Days = recentWellbeing.size();
        long painEventsLast7Days = 0L;
        long painSeveritySum = 0L;
        long painSeverityCount = 0L;
        long painEventsToday = 0L;
        long painSeveritySumToday = 0L;
        long painSeverityCountToday = 0L;
        Map<String, Long> painByBodyArea = new HashMap<>();

        Map<Integer, Long> moodDistribution = new HashMap<>();
        Map<LocalDate, List<Integer>> painByDate = new HashMap<>();

        for (WellbeingEntryEntity w : recentWellbeing) {
            String symptom = w.getSymptomType();
            LocalDate date = w.getCreatedAt().atZone(zone).toLocalDate();
            boolean isToday = !date.isBefore(today);

            if (includePain && symptom != null && symptom.equalsIgnoreCase("PAIN")) {
                painEventsLast7Days++;
                if (isToday) painEventsToday++;
                String area = w.getBodyArea();
                String key = (area == null || area.isBlank()) ? "UNKNOWN" : area.trim().toUpperCase(Locale.ROOT);
                painByBodyArea.put(key, painByBodyArea.getOrDefault(key, 0L) + 1L);
                if (w.getSeverity() != null) {
                    painSeveritySum += w.getSeverity();
                    painSeverityCount++;
                    if (isToday) {
                        painSeveritySumToday += w.getSeverity();
                        painSeverityCountToday++;
                    }
                    painByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(w.getSeverity());
                }
            } else if (symptom == null && w.getMoodScore() != null) {
                moodDistribution.merge(w.getMoodScore(), 1L, Long::sum);
            }
        }

        Double avgSeverity = null;
        if (includePain && painSeverityCount > 0) {
            avgSeverity = painSeveritySum / (double) painSeverityCount;
        }

        Double avgSeverityToday = null;
        if (includePain && painSeverityCountToday > 0) {
            avgSeverityToday = painSeveritySumToday / (double) painSeverityCountToday;
        }

        List<PainSeverityDataPoint> painSeverityTimeSeries = includePain
            ? buildPainTimeSeries(since, now, zone, painByDate)
            : List.of();

        Map<Integer, Double> moodPercent = computePercentages(moodDistribution);
        Map<String, Double> painPercent = includePain ? computePercentagesString(painByBodyArea) : Map.of();
        List<CaregiverDashboardResponse.MoodChartItem> moodChart = buildMoodChartItems(moodDistribution, moodPercent);
        List<CaregiverDashboardResponse.PainChartItem> painChart = includePain
            ? buildPainChartItems(painByBodyArea, painPercent)
            : List.of();

        return new CaregiverDashboardResponse(
            normalizePeriod(period),
            since,
            profile.getDisplayName(),
            profile.getFavFood(),
            profile.getFavDrink(),
            profile.getFavShow(),
            byBucket,
            recentInteractions.size(),
            todayInteractions,
            wellbeingEntriesLast7Days,
            painEventsLast7Days,
            avgSeverity,
            painEventsToday,
            avgSeverityToday,
            painByBodyArea,
            moodDistribution,
            moodPercent,
            painPercent,
            moodChart,
            painChart,
            painSeverityTimeSeries
        );
    }

    private static final Map<Integer, String> MOOD_LABELS = Map.of(
        1, "Very sad", 2, "Sad", 3, "Not sure", 4, "Okay", 5, "Happy"
    );

    private static List<CaregiverDashboardResponse.MoodChartItem> buildMoodChartItems(
            Map<Integer, Long> counts, Map<Integer, Double> percents) {
        if (counts == null || counts.isEmpty()) return List.of();
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) return List.of();
        List<CaregiverDashboardResponse.MoodChartItem> items = new ArrayList<>();
        for (Map.Entry<Integer, Long> e : counts.entrySet()) {
            int score = e.getKey();
            long count = e.getValue();
            double percent = 100.0 * count / total;
            String label = MOOD_LABELS.getOrDefault(score, "Mood " + score);
            items.add(new CaregiverDashboardResponse.MoodChartItem(score, label, count, percent));
        }
        items.sort(Comparator.comparingInt(CaregiverDashboardResponse.MoodChartItem::score));
        return items;
    }

    private static List<CaregiverDashboardResponse.PainChartItem> buildPainChartItems(
            Map<String, Long> counts, Map<String, Double> percents) {
        if (counts == null || counts.isEmpty()) return List.of();
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) return List.of();
        List<CaregiverDashboardResponse.PainChartItem> items = new ArrayList<>();
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            String area = e.getKey();
            long count = e.getValue();
            double percent = 100.0 * count / total;
            items.add(new CaregiverDashboardResponse.PainChartItem(area, count, percent));
        }
        items.sort(Comparator.comparing(CaregiverDashboardResponse.PainChartItem::bodyArea));
        return items;
    }

    private static Map<Integer, Double> computePercentages(Map<Integer, Long> counts) {
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) return Map.of();
        Map<Integer, Double> result = new HashMap<>();
        for (Map.Entry<Integer, Long> e : counts.entrySet()) {
            result.put(e.getKey(), 100.0 * e.getValue() / total);
        }
        return result;
    }

    private static Map<String, Double> computePercentagesString(Map<String, Long> counts) {
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) return Map.of();
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            result.put(e.getKey(), 100.0 * e.getValue() / total);
        }
        return result;
    }

    private static List<PainSeverityDataPoint> buildPainTimeSeries(
            Instant since, Instant now, ZoneId zone,
            Map<LocalDate, List<Integer>> painByDate) {
        List<PainSeverityDataPoint> series = new ArrayList<>();
        LocalDate start = since.atZone(zone).toLocalDate();
        LocalDate end = now.atZone(zone).toLocalDate();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            List<Integer> severities = painByDate.get(d);
            double avg = severities != null && !severities.isEmpty()
                    ? severities.stream().mapToInt(Integer::intValue).average().orElse(0)
                    : 0;
            series.add(new PainSeverityDataPoint(d.toString(), avg));
        }
        return series;
    }

    private static TimeBucket bucketForInstant(Instant instant, ZoneId zone) {
        LocalTime time = instant.atZone(zone).toLocalTime();
        int hour = time.getHour();

        if (hour >= 6 && hour < 12) {
            return TimeBucket.MORNING;
        } else if (hour >= 12 && hour < 18) {
            return TimeBucket.AFTERNOON;
        } else if (hour >= 18 && hour < 23) {
            return TimeBucket.EVENING;
        } else {
            return TimeBucket.NIGHT;
        }
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

