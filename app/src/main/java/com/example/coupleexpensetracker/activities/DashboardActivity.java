package com.example.coupleexpensetracker.activities;

import android.os.Bundle;
import android.widget.*;

import com.example.coupleexpensetracker.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.*;

public class DashboardActivity extends BaseActivity {

    Spinner spUser;
    TextView tvBalance;
    PieChart pieChart;

    FirebaseFirestore db;

    List<String> userIds = new ArrayList<>();
    List<String> userNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupNavigation(R.layout.activity_dashboard, R.id.nav_dashboard);

        spUser = findViewById(R.id.spUser);
        tvBalance = findViewById(R.id.tvBalance);
        pieChart = findViewById(R.id.pieChart);

        db = FirebaseFirestore.getInstance();
        loadUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!userIds.isEmpty()) {
            loadDashboard(userIds.get(spUser.getSelectedItemPosition()));
        }
    }

    // ---------------- LOAD USERS ----------------
    private void loadUsers() {

        db.collection("users")
                .get()
                .addOnSuccessListener(snapshot -> {

                    userIds.clear();
                    userNames.clear();

                    for (DocumentSnapshot d : snapshot) {

                        // 🔥 THIS MUST MATCH AUTH UID
                        userIds.add(d.getId());
                        userNames.add(d.getString("name"));
                    }

                    ArrayAdapter<String> adapter =
                            new ArrayAdapter<>(
                                    this,
                                    android.R.layout.simple_spinner_item,
                                    userNames
                            );

                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );

                    spUser.setAdapter(adapter);

                    spUser.setOnItemSelectedListener(
                            new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(
                                        AdapterView<?> parent,
                                        android.view.View view,
                                        int position,
                                        long id
                                ) {
                                    loadDashboard(userIds.get(position)); // 🔥 CORRECT UID
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {}
                            }
                    );

                    if (!userIds.isEmpty()) {
                        loadDashboard(userIds.get(0));
                    }
                });
    }


    // ---------------- DASHBOARD LOGIC ----------------
    private void loadDashboard(String userId) {

        final double[] incomeTotal = {0};
        final double[] savingsTotal = {0};
        final double[] expenseTotal = {0};

        Map<String, Double> expenseCategoryMap = new HashMap<>();

        // -------- INCOME --------
        db.collection("income")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(incomeSnap -> {

                    for (var d : incomeSnap) {
                        Double amt = d.getDouble("amount");
                        if (amt != null) incomeTotal[0] += amt;
                    }

                    // -------- SAVINGS --------
                    db.collection("savings")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(savingsSnap -> {

                                for (var d : savingsSnap) {
                                    Double amt = d.getDouble("amount");
                                    if (amt != null) savingsTotal[0] += amt;
                                }

                                // -------- EXPENSE --------
                                db.collection("expenses")
                                        .whereEqualTo("userId", userId)
                                        .get()
                                        .addOnSuccessListener(expSnap -> {

                                            for (var d : expSnap) {
                                                Double amt = d.getDouble("amount");
                                                String cat = d.getString("category");

                                                if (amt == null || cat == null) continue;

                                                expenseTotal[0] += amt;

                                                expenseCategoryMap.put(
                                                        cat,
                                                        expenseCategoryMap.getOrDefault(cat, 0.0) + amt
                                                );
                                            }

                                            // -------- BALANCE --------
                                            double balance =
                                                    (incomeTotal[0] + savingsTotal[0])
                                                            - expenseTotal[0];
                                            DecimalFormat df = new DecimalFormat("#.00");

                                            tvBalance.setText("₹ " + df.format(balance));

                                            drawPieChart(
                                                    incomeTotal[0] + savingsTotal[0],
                                                    expenseCategoryMap
                                            );
                                        });
                            });
                });
    }

    // ---------------- PIE CHART ----------------
    private void drawPieChart(
            double incomeSavings,
            Map<String, Double> expenseCategories
    ) {

        List<PieEntry> entries = new ArrayList<>();

        if (incomeSavings > 0) {
            entries.add(
                    new PieEntry(
                            (float) incomeSavings,
                            "Income + Savings"
                    )
            );
        }

        for (String cat : expenseCategories.keySet()) {
            entries.add(
                    new PieEntry(
                            expenseCategories.get(cat).floatValue(),
                            cat
                    )
            );
        }

        PieDataSet dataSet =
                new PieDataSet(entries, "Financial Overview");

        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }
}
