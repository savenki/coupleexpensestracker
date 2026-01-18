package com.example.coupleexpensetracker;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.coupleexpensetracker.activities.DashboardActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Directly open Dashboard or Login
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }
}
