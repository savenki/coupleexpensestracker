package com.example.coupleexpensetracker.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import com.example.coupleexpensetracker.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.*;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class DashboardActivity extends BaseActivity {

    Spinner spMonth, spYear, spUser;
    TextView tvBalance, tvWarning, tvToggle;
    LinearLayout layoutBreakdown;
    PieChart pieChart;

    FirebaseFirestore db;

    List<String> userIds = new ArrayList<>();
    List<String> userNames = new ArrayList<>();

    boolean isReady = false;

    // Your existing date format: "1/1/2026"
    SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ VERY IMPORTANT (keeps drawer + bottom nav)
        setupNavigation(R.layout.activity_dashboard, R.id.nav_dashboard);

        db = FirebaseFirestore.getInstance();

        bindViews();
        setupMonthYear();
        loadUsers();
    }

    // ---------------- VIEW BINDING ----------------
    private void bindViews() {
        spMonth = findViewById(R.id.spMonth);
        spYear = findViewById(R.id.spYear);
        spUser = findViewById(R.id.spUser);
        tvBalance = findViewById(R.id.tvBalance);
        tvWarning = findViewById(R.id.tvWarning);
        tvToggle = findViewById(R.id.tvToggle);
        layoutBreakdown = findViewById(R.id.layoutBreakdown);
        pieChart = findViewById(R.id.pieChart);

        tvToggle.setOnClickListener(v -> {
            boolean open = layoutBreakdown.getVisibility() == View.VISIBLE;
            layoutBreakdown.setVisibility(open ? View.GONE : View.VISIBLE);
            tvToggle.setText(open ? "Expense Breakdown ▼" : "Expense Breakdown ▲");
        });
    }

    // ---------------- MONTH / YEAR ----------------
    private void setupMonthYear() {

        String[] months = {
                "January","February","March","April","May","June",
                "July","August","September","October","November","December"
        };

        List<Integer> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = currentYear - 2; i <= currentYear + 2; i++) {
            years.add(i);
        }

        spMonth.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, months));

        spYear.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, years));
    }

    // ---------------- USERS ----------------
    private void loadUsers() {

        db.collection("users").get().addOnSuccessListener(snapshot -> {

            userIds.clear();
            userNames.clear();

            for (DocumentSnapshot d : snapshot) {
                userIds.add(d.getId());
                userNames.add(d.getString("name"));
            }

            spUser.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, userNames));

            AdapterView.OnItemSelectedListener listener =
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                            if (isReady && !userIds.isEmpty()) {
                                loadDashboard(userIds.get(spUser.getSelectedItemPosition()));
                            }
                        }
                        @Override public void onNothingSelected(AdapterView<?> parent) {}
                    };

            spMonth.setOnItemSelectedListener(listener);
            spYear.setOnItemSelectedListener(listener);
            spUser.setOnItemSelectedListener(listener);

            isReady = true;

            if (!userIds.isEmpty()) {
                loadDashboard(userIds.get(0));
            }
        });
    }

    // ---------------- DASHBOARD ----------------
    private void loadDashboard(String userId) {

        layoutBreakdown.removeAllViews();
        pieChart.clear();

        int selMonth = spMonth.getSelectedItemPosition(); // 0-based
        int selYear = Integer.parseInt(spYear.getSelectedItem().toString());

        double[] income = {0};
        double[] savings = {0};
        double[] expense = {0};

        Map<String, Double> categoryMap = new HashMap<>();

        // -------- INCOME --------
        db.collection("income")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(incomeSnap -> {

                    for (DocumentSnapshot d : incomeSnap) {
                        try {
                            Date date = sdf.parse(d.getString("date"));
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(date);

                            if (cal.get(Calendar.MONTH) == selMonth &&
                                    cal.get(Calendar.YEAR) == selYear) {

                                Double amt = d.getDouble("amount");
                                if (amt != null) income[0] += amt;
                            }
                        } catch (Exception ignored) {}
                    }

                    // -------- SAVINGS (Timestamp) --------
                    db.collection("savings")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(snap -> {

                                for (DocumentSnapshot d : snap) {
                                    if (d.getTimestamp("createdAt") == null) continue;

                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(d.getTimestamp("createdAt").toDate());

                                    if (cal.get(Calendar.MONTH) == selMonth &&
                                            cal.get(Calendar.YEAR) == selYear) {

                                        Double amt = d.getDouble("amount");
                                        if (amt != null) savings[0] += amt;
                                    }
                                }

                                // -------- EXPENSE --------
                                db.collection("expenses")
                                        .whereEqualTo("userId", userId)
                                        .get()
                                        .addOnSuccessListener(expSnap -> {

                                            for (DocumentSnapshot d : expSnap) {
                                                try {
                                                    Date date = sdf.parse(d.getString("date"));
                                                    Calendar cal = Calendar.getInstance();
                                                    cal.setTime(date);

                                                    if (cal.get(Calendar.MONTH) == selMonth &&
                                                            cal.get(Calendar.YEAR) == selYear) {

                                                        Double amt = d.getDouble("amount");
                                                        String cat = d.getString("category");
                                                        if (amt == null || cat == null) continue;

                                                        expense[0] += amt;
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                            categoryMap.put(cat,
                                                                    categoryMap.getOrDefault(cat, 0.0) + amt);
                                                        }
                                                    }
                                                } catch (Exception ignored) {}
                                            }

                                            updateUI(income[0], savings[0], expense[0], categoryMap);
                                        });
                            });
                });
    }

    // ---------------- UI UPDATE ----------------
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

    // ---------------- PIE CHART ----------------
    private void drawPieChart(double income, double savings,
                              Map<String, Double> catMap) {

        List<PieEntry> entries = new ArrayList<>();

        if (income > 0) entries.add(new PieEntry((float) income, "Income"));
        if (savings > 0) entries.add(new PieEntry((float) savings, "Savings"));

        for (String cat : catMap.keySet()) {
            entries.add(new PieEntry(catMap.get(cat).floatValue(), cat));
        }

        PieDataSet ds = new PieDataSet(entries, "Overview");
        ds.setColors(ColorTemplate.MATERIAL_COLORS);
        ds.setValueTextSize(12f);

        pieChart.setData(new PieData(ds));
        pieChart.invalidate();
    }

    // ---------------- CATEGORY BREAKDOWN ----------------
    private void buildBreakdown(Map<String, Double> catMap, double income) {

        for (String cat : catMap.keySet()) {

            View v = LayoutInflater.from(this)
                    .inflate(R.layout.item_category_progress, layoutBreakdown, false);

            TextView tvCat = v.findViewById(R.id.tvCategory);
            TextView tvAmt = v.findViewById(R.id.tvAmount);
            ProgressBar pb = v.findViewById(R.id.progressBar);

            double amt = catMap.get(cat);

            tvCat.setText(cat);
            tvAmt.setText("₹ " + new DecimalFormat("#.00").format(amt));

            pb.setMax((int) income);
            pb.setProgress((int) amt);

            layoutBreakdown.addView(v);
        }
    }
}
