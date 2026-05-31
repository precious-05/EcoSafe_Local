package com.example.eco_front;

import android.graphics.Color;
import android.graphics.Typeface;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.ArrayList;
import java.util.List;

final class RiskChartHelper {

    static final int COLOR_FIRE = Color.parseColor("#FF6B35");
    static final int COLOR_SAFE = Color.parseColor("#4ECDC4");
    static final int COLOR_ACCENT = Color.parseColor("#A78BFA");
    static final int COLOR_GOLD = Color.parseColor("#D4A373");
    static final int COLOR_GRID = Color.parseColor("#D8ECE3");
    static final int COLOR_TEXT = Color.parseColor("#1B4332");

    private RiskChartHelper() {
    }

    static void stylePieChart(PieChart chart) {
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setTransparentCircleColor(Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.setDrawEntryLabels(true);
        chart.setEntryLabelColor(COLOR_TEXT);
        chart.setEntryLabelTextSize(11f);
        chart.setHoleRadius(58f);
        chart.setTransparentCircleRadius(62f);
        chart.setCenterTextColor(COLOR_TEXT);
        chart.setCenterTextSize(14f);
        chart.setRotationEnabled(false);
        chart.setHighlightPerTapEnabled(true);

        Legend legend = chart.getLegend();
        legend.setTextColor(COLOR_TEXT);
        legend.setTextSize(11f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
    }

    static void bindFireSafePie(PieChart chart, RiskAnalyticsSnapshot data) {
        List<PieEntry> entries = new ArrayList<>();
        if (data.fireCount > 0) {
            entries.add(new PieEntry(data.fireCount, "Fire"));
        }
        if (data.safeCount > 0) {
            entries.add(new PieEntry(data.safeCount, "Safe"));
        }
        if (entries.isEmpty()) {
            entries.add(new PieEntry(1f, "No data"));
        }

        PieDataSet set = new PieDataSet(entries, "");
        List<Integer> colors = new ArrayList<>();
        if (data.fireCount > 0) {
            colors.add(COLOR_FIRE);
        }
        if (data.safeCount > 0) {
            colors.add(COLOR_SAFE);
        }
        if (colors.isEmpty()) {
            colors.add(Color.parseColor("#D8ECE3"));
        }
        set.setColors(colors);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(12f);
        set.setSliceSpace(3f);

        PieData pieData = new PieData(set);
        pieData.setValueFormatter(new PercentFormatter(chart));
        chart.setData(pieData);
        chart.setCenterText(data.hasData()
                ? data.fireCount + " fire\n" + data.safeCount + " safe"
                : "No data");
        chart.invalidate();
    }

    static void bindRiskGaugePie(PieChart chart, int riskScore) {
        float remainder = Math.max(0f, 100f - riskScore);
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(riskScore, "Risk"));
        entries.add(new PieEntry(remainder, ""));

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(new int[]{gaugeColor(riskScore), Color.parseColor("#D8ECE3")});
        set.setDrawValues(false);

        chart.setData(new PieData(set));
        chart.setCenterText(riskScore + "\nRisk Score");
        chart.setDrawEntryLabels(false);
        chart.getLegend().setEnabled(false);
        chart.invalidate();
    }

    static int gaugeColor(int score) {
        if (score >= 65) {
            return COLOR_FIRE;
        }
        if (score >= 35) {
            return Color.parseColor("#FFAB40");
        }
        return Color.parseColor("#69F0AE");
    }

    static void styleBarChart(BarChart chart) {
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setFitBars(true);
        chart.setExtraOffsets(8f, 8f, 8f, 8f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(COLOR_TEXT);
        xAxis.setGridColor(COLOR_GRID);
        xAxis.setGranularity(1f);
        xAxis.setDrawAxisLine(false);

        YAxis left = chart.getAxisLeft();
        left.setTextColor(COLOR_TEXT);
        left.setGridColor(COLOR_GRID);
        left.setAxisMinimum(0f);
        left.setGranularity(1f);
        left.setDrawAxisLine(false);

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setTextColor(COLOR_TEXT);
    }

    static void bindWeeklyBarChart(BarChart chart, RiskAnalyticsSnapshot data) {
        List<BarEntry> fireBars = new ArrayList<>();
        List<BarEntry> safeBars = new ArrayList<>();

        for (int i = 0; i < data.last7DayLabels.size(); i++) {
            int fires = i < data.last7DayFires.size() ? data.last7DayFires.get(i) : 0;
            int total = i < data.last7DayTotals.size() ? data.last7DayTotals.get(i) : 0;
            int safe = Math.max(0, total - fires);
            fireBars.add(new BarEntry(i, fires));
            safeBars.add(new BarEntry(i, safe));
        }

        BarDataSet fireSet = new BarDataSet(fireBars, "Fire");
        fireSet.setColor(COLOR_FIRE);
        fireSet.setValueTextColor(Color.WHITE);
        fireSet.setValueTextSize(10f);

        BarDataSet safeSet = new BarDataSet(safeBars, "Safe");
        safeSet.setColor(COLOR_SAFE);
        safeSet.setValueTextColor(Color.WHITE);
        safeSet.setValueTextSize(10f);

        BarData barData = new BarData(fireSet, safeSet);
        barData.setBarWidth(0.35f);
        float groupSpace = 0.2f;
        float barSpace = 0.05f;
        barData.groupBars(0f, groupSpace, barSpace);

        chart.setData(barData);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(data.last7DayLabels));
        chart.getXAxis().setCenterAxisLabels(true);
        chart.getXAxis().setAxisMinimum(0f);
        float groupWidth = barData.getGroupWidth(groupSpace, barSpace);
        chart.getXAxis().setAxisMaximum(groupWidth * data.last7DayLabels.size());
        chart.invalidate();
    }

    static void styleLineChart(LineChart chart) {
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(COLOR_TEXT);
        xAxis.setGridColor(COLOR_GRID);
        xAxis.setDrawAxisLine(false);

        YAxis left = chart.getAxisLeft();
        left.setTextColor(COLOR_TEXT);
        left.setGridColor(COLOR_GRID);
        left.setAxisMinimum(0f);
        left.setAxisMaximum(100f);
        left.setDrawAxisLine(false);

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setTextColor(COLOR_TEXT);
    }

    static void bindConfidenceLine(LineChart chart, RiskAnalyticsSnapshot data) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < data.last7DayAvgConfidence.size(); i++) {
            entries.add(new Entry(i, data.last7DayAvgConfidence.get(i)));
        }

        LineDataSet set = new LineDataSet(entries, "Avg confidence %");
        set.setColor(COLOR_ACCENT);
        set.setCircleColor(COLOR_GOLD);
        set.setLineWidth(2.5f);
        set.setCircleRadius(4f);
        set.setDrawValues(true);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setFillColor(Color.parseColor("#554ECDC4"));
        set.setValueTypeface(Typeface.DEFAULT_BOLD);

        chart.setData(new LineData(set));
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(data.last7DayLabels));
        chart.invalidate();
    }

    static void styleHorizontalBarChart(HorizontalBarChart chart) {
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setFitBars(true);
        chart.setExtraOffsets(8f, 8f, 16f, 8f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(COLOR_TEXT);
        xAxis.setGridColor(COLOR_GRID);
        xAxis.setAxisMinimum(0f);
        xAxis.setGranularity(1f);
        xAxis.setDrawAxisLine(false);

        YAxis left = chart.getAxisLeft();
        left.setTextColor(COLOR_TEXT);
        left.setDrawAxisLine(false);
        left.setGridColor(Color.TRANSPARENT);

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setTextColor(COLOR_TEXT);
        chart.getLegend().setEnabled(true);
    }

    static void bindLocationZonesBar(HorizontalBarChart chart, RiskAnalyticsSnapshot data) {
        if (data.hotspotLabels.isEmpty()) {
            chart.clear();
            chart.setNoDataText("No geo zones yet");
            chart.setNoDataTextColor(COLOR_TEXT);
            chart.invalidate();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < data.hotspotLabels.size(); i++) {
            int fires = data.hotspotFireCounts.get(i);
            int total = data.hotspotTotalCounts.get(i);
            int safe = Math.max(0, total - fires);
            entries.add(new BarEntry(i, new float[]{fires, safe}));
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(COLOR_FIRE, COLOR_SAFE);
        set.setStackLabels(new String[]{"Fire", "Safe"});
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(10f);
        set.setDrawValues(true);

        BarData barData = new BarData(set);
        barData.setBarWidth(0.55f);
        chart.setData(barData);
        chart.getAxisLeft().setValueFormatter(new IndexAxisValueFormatter(data.hotspotLabels));
        chart.getAxisLeft().setGranularity(1f);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.invalidate();
    }
}
