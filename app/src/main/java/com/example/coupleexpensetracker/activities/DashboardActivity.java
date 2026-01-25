package com.example.coupleexpensetracker.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.core.widget.NestedScrollView;

import com.example.coupleexpensetracker.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class DashboardActivity extends BaseActivity {

    Spinner spMonth, spYear;
    TextView tvBalance, tvWarning, tvToggle;
    LinearLayout layoutBreakdown;
    NestedScrollView nestedScroll;
    PieChart pieChart;

    FirebaseFirestore db;
    String currentUserId;

    boolean isReady = false;

    SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupNavigation(R.layout.activity_dashboard, R.id.nav_dashboard);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        bindViews();
        setupMonthYear();

        isReady = true;
        loadDashboard();
    }

    private void bindViews() {
        spMonth = findViewById(R.id.spMonth);
        spYear = findViewById(R.id.spYear);
        tvBalance = findViewById(R.id.tvBalance);
        tvWarning = findViewById(R.id.tvWarning);
        tvToggle = findViewById(R.id.tvToggle);
        layoutBreakdown = findViewById(R.id.layoutBreakdown);
        nestedScroll = findViewById(R.id.nestedScroll);
        pieChart = findViewById(R.id.pieChart);

        tvToggle.setOnClickListener(v -> {
            boolean open = nestedScroll.getVisibility() == View.VISIBLE;
            nestedScroll.setVisibility(open ? View.GONE : View.VISIBLE);
            tvToggle.setText(open ? "Expense Breakdown ▼" : "Expense Breakdown ▲");
        });

        AdapterView.OnItemSelectedListener reload =
                new AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                        if (isReady) loadDashboard();
                    }
                    @Override public void onNothingSelected(AdapterView<?> p) {}
                };

        spMonth.setOnItemSelectedListener(reload);
        spYear.setOnItemSelectedListener(reload);
    }

    private void setupMonthYear() {

        String[] months = {
                "January","February","March","April","May","June",
                "July","August","September","October","November","December"
        };

        List<Integer> years = new ArrayList<>();
        int y = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = y - 2; i <= y + 2; i++) years.add(i);

        spMonth.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, months));

        spYear.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, years));
    }

    // ---------------- DASHBOARD ----------------
    private void loadDashboard() {

        layoutBreakdown.removeAllViews();
        pieChart.clear();

        int selMonth = spMonth.getSelectedItemPosition();
        int selYear = Integer.parseInt(spYear.getSelectedItem().toString());

        double[] income = {0};
        double[] savings = {0};
        double[] expense = {0};

        Map<String, Double> catMap = new HashMap<>();

        // INCOME
        db.collection("income")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(incomeSnap -> {

                    for (DocumentSnapshot d : incomeSnap) {
                        try {
                            Date date = sdf.parse(d.getString("date"));
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(date);

                            if (cal.get(Calendar.MONTH) == selMonth &&
                                    cal.get(Calendar.YEAR) == selYear) {

                                income[0] += d.getDouble("amount");
                            }
                        } catch (Exception ignored) {}
                    }

                    // SAVINGS
                    db.collection("savings")
                            .whereEqualTo("userId", currentUserId)
                            .get()
                            .addOnSuccessListener(saveSnap -> {

                                for (DocumentSnapshot d : saveSnap) {
                                    if (d.getTimestamp("createdAt") == null) continue;

                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(d.getTimestamp("createdAt").toDate());

                                    if (cal.get(Calendar.MONTH) == selMonth &&
                                            cal.get(Calendar.YEAR) == selYear) {

                                        savings[0] += d.getDouble("amount");
                                    }
                                }

                                // EXPENSE
                                db.collection("expenses")
                                        .whereEqualTo("userId", currentUserId)
                                        .get()
                                        .addOnSuccessListener(expSnap -> {

                                            for (DocumentSnapshot d : expSnap) {
                                                try {
                                                    Date date = sdf.parse(d.getString("date"));
                                                    Calendar cal = Calendar.getInstance();
                                                    cal.setTime(date);

                                                    if (cal.get(Calendar.MONTH) == selMonth &&
                                                            cal.get(Calendar.YEAR) == selYear) {

                                                        double amt = d.getDouble("amount");
                                                        String cat = d.getString("category");

                                                        expense[0] += amt;
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                            catMap.put(cat,
                                                                    catMap.getOrDefault(cat, 0.0) + amt);
                                                        }
                                                    }
                                                } catch (Exception ignored) {}
                                            }

                                            updateUI(income[0], savings[0], expense[0], catMap);
                                        });
                            });
                });
    }

    private void updateUI(double income, double savings,
                          double expense, Map<String, Double> catMap) {

        double balance = (income + savings) - expense;
        tvBalance.setText("₹ " + new DecimalFormat("#.00").format(balance));

        double percent = income == 0 ? 0 : (expense / income) * 100;

        if (percent > 80) {
            tvWarning.setText("⚠ High Spending");
            tvWarning.setTextColor(getColor(android.R.color.holo_red_dark));
        } else if (percent > 60) {
            tvWarning.setText("⚠ Moderate Spending");
            tvWarning.setTextColor(getColor(android.R.color.holo_orange_dark));
        } else {
            tvWarning.setText("✔ Healthy Savings");
            tvWarning.setTextColor(getColor(android.R.color.holo_green_dark));
        }

        drawPieChart(income, savings, catMap);
        buildBreakdown(catMap, income);
    }

    private void drawPieChart(double income, double savings, Map<String, Double> catMap) {

        List<PieEntry> entries = new ArrayList<>();
        if (income > 0) entries.add(new PieEntry((float) income, "Income"));
        if (savings > 0) entries.add(new PieEntry((float) savings, "Savings"));

        for (String c : catMap.keySet()) {
            entries.add(new PieEntry(catMap.get(c).floatValue(), c));
        }

        PieDataSet ds = new PieDataSet(entries, "Overview");
        ds.setColors(ColorTemplate.MATERIAL_COLORS);
        ds.setValueTextSize(12f);

        pieChart.setData(new PieData(ds));
        pieChart.invalidate();
    }

    private void buildBreakdown(Map<String, Double> catMap, double income) {

        for (String cat : catMap.keySet()) {

            View v = LayoutInflater.from(this)
                    .inflate(R.layout.item_category_progress, layoutBreakdown, false);

            ((TextView) v.findViewById(R.id.tvCategory)).setText(cat);
            ((TextView) v.findViewById(R.id.tvAmount))
                    .setText("₹ " + new DecimalFormat("#.00").format(catMap.get(cat)));

            ProgressBar pb = v.findViewById(R.id.progressBar);
            pb.setMax((int) income);
            pb.setProgress(catMap.get(cat).intValue());

            layoutBreakdown.addView(v);
        }
    }
}
