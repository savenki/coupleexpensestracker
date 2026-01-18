package com.example.coupleexpensetracker.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import com.example.coupleexpensetracker.R;
import com.example.coupleexpensetracker.models.Expense;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class ExpenseActivity extends BaseActivity {

    private static final String TAG = "EXPENSE_DEBUG";


    TextInputEditText etDate;
    EditText etAmount, etNewCategory;
    ChipGroup chipGroupCategories;
    MaterialButton btnSaveExpense;

    FirebaseFirestore db;

    // 🔥 USER DATA
    List<String> userIds = new ArrayList<>();
    List<String> userNames = new ArrayList<>();
    String selectedUserId = null;

    String selectedDate = "";
    String selectedCategory = null;
    String userId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNavigation(R.layout.activity_expense, R.id.nav_expense);

        db = FirebaseFirestore.getInstance();



        bindViews();
        setupDatePicker();

        loadCategories();

        btnSaveExpense.setOnClickListener(v -> saveExpense());
    }

    private void bindViews() {
        userId = FirebaseAuth.getInstance().getUid();
        etDate = findViewById(R.id.etDate);
        etAmount = findViewById(R.id.etAmount);
        etNewCategory = findViewById(R.id.etNewCategory);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        btnSaveExpense = findViewById(R.id.btnSaveExpense);
    }

    // ---------------- LOAD USERS ----------------


    // ---------------- DATE PICKER ----------------
    private void setupDatePicker() {
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, y, m, d) -> {
                        selectedDate = d + "/" + (m + 1) + "/" + y;
                        etDate.setText(selectedDate);
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    // ---------------- LOAD CATEGORIES ----------------
    private void loadCategories() {

        db.collection("expense_categories")
                .get()
                .addOnSuccessListener(snapshot -> {

                    chipGroupCategories.removeAllViews();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String name = doc.getString("name");
                        if (name != null) addChip(name);
                    }
                });
    }

    private void addChip(String name) {
        Chip chip = new Chip(this);
        chip.setText(name);
        chip.setCheckable(true);

        chip.setOnCheckedChangeListener((c, checked) -> {
            if (checked) selectedCategory = name;
        });

        chipGroupCategories.addView(chip);
    }

    // ---------------- SAVE EXPENSE ----------------
    private void saveExpense() {



        String amountStr = etAmount.getText().toString().trim();
        String newCategory = etNewCategory.getText().toString().trim();

        if (selectedDate.isEmpty() || amountStr.isEmpty()) {
            toast("Fill all fields");
            return;
        }

        String finalCategory;

        if (!newCategory.isEmpty()) {
            finalCategory = newCategory;
            saveCategory(newCategory);
        } else if (selectedCategory != null) {
            finalCategory = selectedCategory;
        } else {
            toast("Select category");
            return;
        }

        double amount = Double.parseDouble(amountStr);

        Expense expense = new Expense();
        expense.userId = userId;   // ✅ MUST
        expense.category = finalCategory;
        expense.amount = amount;
        expense.date = selectedDate;
        expense.createdAt = System.currentTimeMillis();


        db.collection("expenses")
                .add(expense)
                .addOnSuccessListener(d -> {
                    toast("Expense saved");
                    clearForm();
                });
    }

    private void saveCategory(String name) {
        Map<String, Object> cat = new HashMap<>();
        cat.put("name", name);
        db.collection("expense_categories").add(cat);
    }

    private void clearForm() {
        etDate.setText("");
        etAmount.setText("");
        etNewCategory.setText("");
        chipGroupCategories.clearCheck();
        selectedCategory = null;
        selectedDate = "";
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
