package com.example.coupleexpensetracker.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.*;

import com.example.coupleexpensetracker.R;
import com.example.coupleexpensetracker.models.Income;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class IncomeActivity extends BaseActivity {

    String userId;
    TextInputEditText etDate, etAmount;
    Spinner spCategory;
    MaterialButton btnSaveIncome;
    PieChart pieChart;

    FirebaseFirestore db;

    List<String> userIds = new ArrayList<>();
    List<String> userNames = new ArrayList<>();
    String selectedUserId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNavigation(R.layout.activity_income, R.id.nav_income);

        db = FirebaseFirestore.getInstance();

        userId = FirebaseAuth.getInstance().getUid();

        etDate = findViewById(R.id.etDate);
        etAmount = findViewById(R.id.etAmount);
        spCategory = findViewById(R.id.spCategory);
        btnSaveIncome = findViewById(R.id.btnSaveIncome);
        pieChart = findViewById(R.id.incomeChart);
        setupCategorySpinner();

        etDate.setOnClickListener(v -> showDatePicker());
        btnSaveIncome.setOnClickListener(v -> saveIncome());
    }



    private void setupCategorySpinner() {
        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(
                        this,
                        R.array.income_categories,
                        android.R.layout.simple_spinner_item
                );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this,
                (v, y, m, d) -> etDate.setText(d + "/" + (m + 1) + "/" + y),
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void saveIncome() {


        String date = etDate.getText().toString();
        String amtStr = etAmount.getText().toString();

        if (date.isEmpty() || amtStr.isEmpty()) {
            toast("Fill all fields");
            return;
        }

        double amount = Double.parseDouble(amtStr);

        Income income = new Income();
        income.userId = userId;   // ✅ MUST
        income.date = date;
        income.category = spCategory.getSelectedItem().toString();
        income.amount = amount;
        income.createdAt = System.currentTimeMillis();


        db.collection("income")
                .add(income)
                .addOnSuccessListener(d -> {
                    toast("Income saved");
                    etAmount.setText("");
                    loadIncomeChart();
                });
    }

    private void loadIncomeChart() {

        if (selectedUserId == null) return;

        db.collection("income")
                .whereEqualTo("userId", selectedUserId)
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) {
                        pieChart.clear();
                        return;
                    }

                    Map<String, Float> totals = new HashMap<>();

                    for (DocumentSnapshot d : query) {
                        String cat = d.getString("category");
                        Double amt = d.getDouble("amount");
                        if (cat == null || amt == null) continue;

                        totals.put(cat,
                                totals.getOrDefault(cat, 0f) + amt.floatValue());
                    }

                    List<PieEntry> entries = new ArrayList<>();
                    for (String k : totals.keySet())
                        entries.add(new PieEntry(totals.get(k), k));

                    PieDataSet ds = new PieDataSet(entries, "Income");
                    ds.setColors(ColorTemplate.MATERIAL_COLORS);

                    pieChart.setData(new PieData(ds));
                    pieChart.invalidate();
                });
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
