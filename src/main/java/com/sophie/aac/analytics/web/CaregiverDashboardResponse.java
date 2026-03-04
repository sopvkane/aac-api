package com.sophie.aac.analytics.web;

import com.sophie.aac.suggestions.domain.TimeBucket;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CaregiverDashboardResponse(
    String period,
    Instant since,
    String displayName,
    String favFood,
    String favDrink,
    String favShow,
    Map<TimeBucket, Long> interactionsByTimeBucket,
    long totalInteractionsLast7Days,
    long todayInteractions,
    long wellbeingEntriesLast7Days,
    long painEventsLast7Days,
    Double averagePainSeverityLast7Days,
    long painEventsToday,
    Double averagePainSeverityToday,
    Map<String, Long> painByBodyArea,
    Map<Integer, Long> moodDistribution,
    Map<Integer, Double> moodDistributionPercent,
    Map<String, Double> painByBodyAreaPercent,
    List<MoodChartItem> moodChartItems,
    List<PainChartItem> painChartItems,
    List<PainSeverityDataPoint> painSeverityTimeSeries
) {
    public record MoodChartItem(int score, String label, long count, double percent) {}
    public record PainChartItem(String bodyArea, long count, double percent) {}
}

