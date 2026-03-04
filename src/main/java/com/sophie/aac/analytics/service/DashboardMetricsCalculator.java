package com.sophie.aac.analytics.service;

import com.sophie.aac.analytics.domain.InteractionEventEntity;
import com.sophie.aac.analytics.domain.WellbeingEntryEntity;
import com.sophie.aac.analytics.web.CaregiverDashboardResponse;
import com.sophie.aac.analytics.web.PainSeverityDataPoint;
import com.sophie.aac.suggestions.domain.TimeBucket;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class DashboardMetricsCalculator {

  private static final Map<Integer, String> MOOD_LABELS = Map.of(
      1, "Very sad", 2, "Sad", 3, "Not sure", 4, "Okay", 5, "Happy"
  );

  private DashboardMetricsCalculator() {
  }

  static DashboardMetrics calculate(
      List<InteractionEventEntity> recentInteractions,
      List<WellbeingEntryEntity> recentWellbeing,
      Instant since,
      Instant now,
      ZoneId zone,
      boolean includePain
  ) {
    LocalDate today = LocalDate.now(zone);
    Instant todayStart = today.atStartOfDay(zone).toInstant();

    Map<TimeBucket, Long> byBucket = initBucketMap();
    long todayInteractions = collectInteractionMetrics(recentInteractions, byBucket, todayStart, zone);

    PainAggregation pain = collectWellbeingMetrics(recentWellbeing, includePain, today, zone);

    Double avgSeverity = includePain && pain.painSeverityCount > 0
        ? pain.painSeveritySum / (double) pain.painSeverityCount
        : null;

    Double avgSeverityToday = includePain && pain.painSeverityCountToday > 0
        ? pain.painSeveritySumToday / (double) pain.painSeverityCountToday
        : null;

    List<PainSeverityDataPoint> painSeverityTimeSeries = includePain
        ? buildPainTimeSeries(since, now, zone, pain.painByDate)
        : List.of();

    Map<Integer, Double> moodPercent = computePercentages(pain.moodDistribution);
    Map<String, Double> painPercent = includePain ? computePercentagesString(pain.painByBodyArea) : Map.of();

    return new DashboardMetrics(
        byBucket,
        recentInteractions.size(),
        todayInteractions,
        recentWellbeing.size(),
        pain.painEventsLast7Days,
        avgSeverity,
        pain.painEventsToday,
        avgSeverityToday,
        pain.painByBodyArea,
        pain.moodDistribution,
        moodPercent,
        painPercent,
        buildMoodChartItems(pain.moodDistribution),
        includePain ? buildPainChartItems(pain.painByBodyArea) : List.of(),
        painSeverityTimeSeries
    );
  }

  private static Map<TimeBucket, Long> initBucketMap() {
    Map<TimeBucket, Long> byBucket = new EnumMap<>(TimeBucket.class);
    for (TimeBucket bucket : TimeBucket.values()) {
      byBucket.put(bucket, 0L);
    }
    return byBucket;
  }

  private static long collectInteractionMetrics(
      List<InteractionEventEntity> recentInteractions,
      Map<TimeBucket, Long> byBucket,
      Instant todayStart,
      ZoneId zone
  ) {
    long todayInteractions = 0L;
    for (InteractionEventEntity interaction : recentInteractions) {
      TimeBucket bucket = bucketForInstant(interaction.getCreatedAt(), zone);
      byBucket.put(bucket, byBucket.get(bucket) + 1);
      if (!interaction.getCreatedAt().isBefore(todayStart)) {
        todayInteractions++;
      }
    }
    return todayInteractions;
  }

  private static PainAggregation collectWellbeingMetrics(
      List<WellbeingEntryEntity> recentWellbeing,
      boolean includePain,
      LocalDate today,
      ZoneId zone
  ) {
    long painEventsLast7Days = 0L;
    long painSeveritySum = 0L;
    long painSeverityCount = 0L;
    long painEventsToday = 0L;
    long painSeveritySumToday = 0L;
    long painSeverityCountToday = 0L;
    Map<String, Long> painByBodyArea = new HashMap<>();
    Map<Integer, Long> moodDistribution = new HashMap<>();
    Map<LocalDate, List<Integer>> painByDate = new HashMap<>();

    for (WellbeingEntryEntity wellbeing : recentWellbeing) {
      String symptom = wellbeing.getSymptomType();
      LocalDate date = wellbeing.getCreatedAt().atZone(zone).toLocalDate();
      boolean isToday = !date.isBefore(today);

      if (includePain && symptom != null && symptom.equalsIgnoreCase("PAIN")) {
        painEventsLast7Days++;
        if (isToday) painEventsToday++;

        String area = wellbeing.getBodyArea();
        String areaKey = (area == null || area.isBlank()) ? "UNKNOWN" : area.trim().toUpperCase(Locale.ROOT);
        painByBodyArea.put(areaKey, painByBodyArea.getOrDefault(areaKey, 0L) + 1L);

        if (wellbeing.getSeverity() != null) {
          int severity = wellbeing.getSeverity();
          painSeveritySum += severity;
          painSeverityCount++;
          if (isToday) {
            painSeveritySumToday += severity;
            painSeverityCountToday++;
          }
          painByDate.computeIfAbsent(date, ignored -> new ArrayList<>()).add(severity);
        }
      } else if (symptom == null && wellbeing.getMoodScore() != null) {
        moodDistribution.merge(wellbeing.getMoodScore(), 1L, Long::sum);
      }
    }

    return new PainAggregation(
        painEventsLast7Days,
        painSeveritySum,
        painSeverityCount,
        painEventsToday,
        painSeveritySumToday,
        painSeverityCountToday,
        painByBodyArea,
        moodDistribution,
        painByDate
    );
  }

  private static List<CaregiverDashboardResponse.MoodChartItem> buildMoodChartItems(Map<Integer, Long> counts) {
    if (counts == null || counts.isEmpty()) return List.of();
    long total = counts.values().stream().mapToLong(Long::longValue).sum();
    if (total == 0) return List.of();

    List<CaregiverDashboardResponse.MoodChartItem> items = new ArrayList<>();
    for (Map.Entry<Integer, Long> entry : counts.entrySet()) {
      int score = entry.getKey();
      long count = entry.getValue();
      double percent = 100.0 * count / total;
      String label = MOOD_LABELS.getOrDefault(score, "Mood " + score);
      items.add(new CaregiverDashboardResponse.MoodChartItem(score, label, count, percent));
    }
    items.sort(Comparator.comparingInt(CaregiverDashboardResponse.MoodChartItem::score));
    return items;
  }

  private static List<CaregiverDashboardResponse.PainChartItem> buildPainChartItems(Map<String, Long> counts) {
    if (counts == null || counts.isEmpty()) return List.of();
    long total = counts.values().stream().mapToLong(Long::longValue).sum();
    if (total == 0) return List.of();

    List<CaregiverDashboardResponse.PainChartItem> items = new ArrayList<>();
    for (Map.Entry<String, Long> entry : counts.entrySet()) {
      String area = entry.getKey();
      long count = entry.getValue();
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
    for (Map.Entry<Integer, Long> entry : counts.entrySet()) {
      result.put(entry.getKey(), 100.0 * entry.getValue() / total);
    }
    return result;
  }

  private static Map<String, Double> computePercentagesString(Map<String, Long> counts) {
    long total = counts.values().stream().mapToLong(Long::longValue).sum();
    if (total == 0) return Map.of();

    Map<String, Double> result = new HashMap<>();
    for (Map.Entry<String, Long> entry : counts.entrySet()) {
      result.put(entry.getKey(), 100.0 * entry.getValue() / total);
    }
    return result;
  }

  private static List<PainSeverityDataPoint> buildPainTimeSeries(
      Instant since,
      Instant now,
      ZoneId zone,
      Map<LocalDate, List<Integer>> painByDate
  ) {
    List<PainSeverityDataPoint> series = new ArrayList<>();
    LocalDate start = since.atZone(zone).toLocalDate();
    LocalDate end = now.atZone(zone).toLocalDate();

    for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
      List<Integer> severities = painByDate.get(date);
      double avg = severities != null && !severities.isEmpty()
          ? severities.stream().mapToInt(Integer::intValue).average().orElse(0)
          : 0;
      series.add(new PainSeverityDataPoint(date.toString(), avg));
    }
    return series;
  }

  private static TimeBucket bucketForInstant(Instant instant, ZoneId zone) {
    int hour = instant.atZone(zone).toLocalTime().getHour();
    if (hour >= 6 && hour < 12) return TimeBucket.MORNING;
    if (hour >= 12 && hour < 18) return TimeBucket.AFTERNOON;
    if (hour >= 18 && hour < 23) return TimeBucket.EVENING;
    return TimeBucket.NIGHT;
  }

  record DashboardMetrics(
      Map<TimeBucket, Long> byBucket,
      long totalInteractions,
      long todayInteractions,
      long wellbeingEntries,
      long painEvents,
      Double averagePainSeverity,
      long painEventsToday,
      Double averagePainSeverityToday,
      Map<String, Long> painByBodyArea,
      Map<Integer, Long> moodDistribution,
      Map<Integer, Double> moodPercent,
      Map<String, Double> painPercent,
      List<CaregiverDashboardResponse.MoodChartItem> moodChart,
      List<CaregiverDashboardResponse.PainChartItem> painChart,
      List<PainSeverityDataPoint> painSeverityTimeSeries
  ) {
  }

  private record PainAggregation(
      long painEventsLast7Days,
      long painSeveritySum,
      long painSeverityCount,
      long painEventsToday,
      long painSeveritySumToday,
      long painSeverityCountToday,
      Map<String, Long> painByBodyArea,
      Map<Integer, Long> moodDistribution,
      Map<LocalDate, List<Integer>> painByDate
  ) {
  }
}
