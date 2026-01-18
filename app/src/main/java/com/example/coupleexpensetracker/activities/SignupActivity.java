package com.example.coupleexpensetracker.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.coupleexpensetracker.R;
import com.example.coupleexpensetracker.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class SignupActivity extends AppCompatActivity {

    TextInputEditText etName, etEmail, etPassword, etDob;
    Spinner spGender;
    CheckBox cbAdmin;
    MaterialButton btnSignup;
    RadioGroup rgGender;


    FirebaseAuth auth;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 🔹 Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // 🔹 Bind Views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etDob = findViewById(R.id.etDob);
        cbAdmin = findViewById(R.id.cbAdmin);
        btnSignup = findViewById(R.id.btnSignup);
        rgGender = findViewById(R.id.rgGender);

        etDob.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                        etDob.setText(date);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });


        // 🔹 Button Click
        btnSignup.setOnClickListener(v -> signupUser());
    }

    private void signupUser() {

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String dob = etDob.getText().toString().trim();

        int selectedId = rgGender.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Select gender", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedGender = findViewById(selectedId);
        String gender = selectedGender.getText().toString();

        boolean isAdmin = cbAdmin.isChecked();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {

                    String uid = result.getUser().getUid();

                    User user = new User();
                    user.uid = uid;
                    user.name = name;
                    user.email = email;
                    user.gender = gender;
                    user.dob = dob;
                    user.isAdmin = isAdmin;
                    user.createdAt = Timestamp.now();

                    firestore.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Signup Successful", Toast.LENGTH_SHORT).show();
                                finish(); // go back to login
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

}
