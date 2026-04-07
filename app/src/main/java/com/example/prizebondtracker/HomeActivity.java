package com.example.prizebondtracker;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;

    // Manager ko field mein rakho taake onDestroy mein stop kar sako
    private BondNotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Android 13+ notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        100
                );
            }
        }

        // ── Notification Manager initialize karo ────────────────────────────
        notificationManager = new BondNotificationManager(this);
        // ────────────────────────────────────────────────────────────────────

        bottomNav = findViewById(R.id.bottomNav);
        loadFragment(new HomeFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment = null;

            if (id == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.nav_my_bonds) {
                fragment = new MyBondsActivity();
            } else if (id == R.id.nav_schedule) {
                fragment = new UpcomingDrawsActivity();
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileActivity();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });

        // Check if opened from notification — profile tab open karo
        if (getIntent() != null &&
                "app_preferences".equals(getIntent().getStringExtra("open_section"))) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
        }
    }

    // ── Memory leak rokne ke liye listener band karo ─────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationManager != null) {
            notificationManager.stopListening();
        }
    }
    // ─────────────────────────────────────────────────────────────────────────

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainContainer, fragment)
                .commit();
    }

    public void showCustomDropdownMenu(View anchorView) {
        android.view.LayoutInflater inflater =
                (android.view.LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View customMenuView = inflater.inflate(R.layout.home_menu, null);

        final android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
                customMenuView,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        customMenuView.findViewById(R.id.menu_add_bond).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, AddBondActivity.class));
            popupWindow.dismiss();
        });
        customMenuView.findViewById(R.id.menu_view_bonds).setOnClickListener(v -> {
            loadFragment(new MyBondsActivity());
            popupWindow.dismiss();
        });
        customMenuView.findViewById(R.id.menu_schedule).setOnClickListener(v -> {
            loadFragment(new UpcomingDrawsActivity());
            popupWindow.dismiss();
        });
        customMenuView.findViewById(R.id.menu_view_results).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, DrawResultsActivity.class));
            popupWindow.dismiss();
        });
        customMenuView.findViewById(R.id.menu_view_probability).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, WinningProbabilityActivity.class));
            popupWindow.dismiss();
        });
        customMenuView.findViewById(R.id.menu_purchasing_suggestions).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, BudgetSuggestionsActivity.class));
            popupWindow.dismiss();
        });
        customMenuView.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            performLogout();
            popupWindow.dismiss();
        });

        popupWindow.showAsDropDown(anchorView, 0, 16, Gravity.START);
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}