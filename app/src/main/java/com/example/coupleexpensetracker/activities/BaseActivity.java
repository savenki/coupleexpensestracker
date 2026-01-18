package com.example.coupleexpensetracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.coupleexpensetracker.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public abstract class BaseActivity extends AppCompatActivity {

    protected void setupNavigation(int layoutResId, int selectedMenuId) {

        setContentView(R.layout.activity_base);

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        NavigationView navigationView = findViewById(R.id.navigationView);
        FrameLayout contentFrame = findViewById(R.id.contentFrame);

        View contentView = LayoutInflater.from(this)
                .inflate(layoutResId, contentFrame, false);
        contentFrame.addView(contentView);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        bottomNav.setSelectedItemId(selectedMenuId);
        navigationView.setCheckedItem(selectedMenuId);

        bottomNav.setOnItemSelectedListener(item -> {
            navigate(item.getItemId());
            return true;
        });

        navigationView.setNavigationItemSelectedListener(item -> {

            drawerLayout.closeDrawers();

            if (item.getItemId() == R.id.nav_logout) {
                logout();   // 🔥 FIX
                return true;
            }

            navigate(item.getItemId());
            return true;
        });
    }

    // ---------------- LOGOUT ----------------
    private void logout() {

        FirebaseAuth.getInstance().signOut();

        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);

        finish();
    }

    // ---------------- NAVIGATION ----------------
    private void navigate(int id) {

        if (id == R.id.nav_dashboard) {
            if (!(this instanceof DashboardActivity)) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
            }
        }
        else if (id == R.id.nav_expense) {
            if (!(this instanceof ExpenseActivity)) {
                startActivity(new Intent(this, ExpenseActivity.class));
                finish();
            }
        }
        else if (id == R.id.nav_income) {
            if (!(this instanceof IncomeActivity)) {
                startActivity(new Intent(this, IncomeActivity.class));
                finish();
            }
        }
        else if (id == R.id.nav_savings) {
            if (!(this instanceof SavingsActivity)) {
                startActivity(new Intent(this, SavingsActivity.class));
                finish();
            }
        }
    }
}
