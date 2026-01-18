package com.example.coupleexpensetracker.activities;

import android.os.Bundle;
import android.widget.*;
import androidx.cardview.widget.CardView;

import com.example.coupleexpensetracker.R;
import com.example.coupleexpensetracker.models.Saving;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.util.*;

public class SavingsActivity extends BaseActivity {

    AutoCompleteTextView spinnerUser;
    EditText etBank, etAmount, etTenure;
    Button btnSave;
    LinearLayout containerSavings;

    FirebaseFirestore db;

    // 🔥 IMPORTANT
    List<String> userNames = new ArrayList<>();
    List<String> userIds = new ArrayList<>();
    String selectedUserId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupNavigation(R.layout.activity_savings, R.id.nav_savings);

        db = FirebaseFirestore.getInstance();

        spinnerUser = findViewById(R.id.spinnerUser);
        etBank = findViewById(R.id.etBank);
        etAmount = findViewById(R.id.etAmount);
        etTenure = findViewById(R.id.etTenure);
        btnSave = findViewById(R.id.btnSave);
        containerSavings = findViewById(R.id.containerSavings);

        loadUsers();

        btnSave.setOnClickListener(v -> saveSavings());
    }

    // ---------------- LOAD USERS ----------------
    private void loadUsers() {

        db.collection("users")
                .get()
                .addOnSuccessListener(snapshot -> {

                    userNames.clear();
                    userIds.clear();

                    for (DocumentSnapshot d : snapshot) {
                        String name = d.getString("name");
                        if (name == null || name.isEmpty()) name = "User";

                        userNames.add(name);
                        userIds.add(d.getId()); // ✅ AUTH UID
                    }

                    if (userNames.isEmpty()) {
                        toast("No users found");
                        return;
                    }

                    ArrayAdapter<String> adapter =
                            new ArrayAdapter<>(
                                    this,
                                    android.R.layout.simple_dropdown_item_1line,
                                    userNames
                            );

                    spinnerUser.setAdapter(adapter);

                    // 🔥 USER SELECT
                    spinnerUser.setOnItemClickListener((parent, view, position, id) -> {
                        selectedUserId = userIds.get(position);
                        loadSavings(selectedUserId);
                    });

                    // 🔥 DEFAULT USER (MANUAL CALL)
                    selectedUserId = userIds.get(0);
                    spinnerUser.setText(userNames.get(0), false);
                    loadSavings(selectedUserId); // 🔥 IMPORTANT
                });
    }


    // ---------------- SAVE SAVINGS ----------------
    private void saveSavings() {

        if (selectedUserId == null) {
            toast("Select user");
            return;
        }

        String bank = etBank.getText().toString().trim();
        String amtStr = etAmount.getText().toString().trim();
        String tenStr = etTenure.getText().toString().trim();

        if (bank.isEmpty() || amtStr.isEmpty() || tenStr.isEmpty()) {
            toast("Fill all fields");
            return;
        }

        double amount;
        int tenure;

        try {
            amount = Double.parseDouble(amtStr);
            tenure = Integer.parseInt(tenStr);
        } catch (Exception e) {
            toast("Invalid amount / tenure");
            return;
        }

        Saving saving = new Saving();
        saving.userId = selectedUserId;                // 🔥 FIX
        saving.userName = spinnerUser.getText().toString();
        saving.bankName = bank;
        saving.amount = amount;
        saving.tenureMonths = tenure;
        saving.createdAt = Timestamp.now();

        db.collection("savings")
                .add(saving)
                .addOnSuccessListener(doc -> {
                    toast("Savings Added");
                    clearForm();
                    loadSavings(selectedUserId);
                });
    }

    // ---------------- LOAD SAVINGS (FILTERED) ----------------
    private void loadSavings(String userId) {

        containerSavings.removeAllViews();

        db.collection("savings")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        toast("No savings for this user");
                        return;
                    }

                    for (DocumentSnapshot d : snapshot) {

                        Double amount = d.getDouble("amount");
                        Long tenureLong = d.getLong("tenureMonths");

                        if (amount == null || tenureLong == null) continue;

                        addSavingsCard(
                                d.getString("bankName"),
                                d.getString("userName"),
                                amount,
                                tenureLong.intValue()
                        );
                    }
                });
    }


    // ---------------- UI CARD ----------------
    private void addSavingsCard(String bank, String user, double amount, int tenure) {

        CardView card = new CardView(this);
        card.setRadius(20);
        card.setCardElevation(8);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32, 32, 32, 32);

        TextView t1 = new TextView(this);
        t1.setText("🏦 Bank: " + bank);

        TextView t2 = new TextView(this);
        t2.setText("👤 User: " + user);

        TextView t3 = new TextView(this);
        t3.setText("💰 Amount: ₹" + amount);

        TextView t4 = new TextView(this);
        t4.setText("📆 Tenure: " + tenure + " months");

        box.addView(t1);
        box.addView(t2);
        box.addView(t3);
        box.addView(t4);

        card.addView(box);
        containerSavings.addView(card);
    }

    private void clearForm() {
        etBank.setText("");
        etAmount.setText("");
        etTenure.setText("");
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
