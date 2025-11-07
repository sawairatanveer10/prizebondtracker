package com.example.prizebondtracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

public class HomeActivity extends AppCompatActivity {

    MaterialCardView cardAI;
    ImageButton btnNotifications;
    MaterialButton btnQuickAdd, btnViewRecommendations;
    BottomNavigationView bottomNav;
    ImageView ivMenuIcon;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // find views
        cardAI = findViewById(R.id.cardAI);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnQuickAdd = findViewById(R.id.btnQuickAdd);
        btnViewRecommendations = findViewById(R.id.btnViewRecommendations);
        bottomNav = findViewById(R.id.bottomNav);
        ivMenuIcon = findViewById(R.id.ivMenuIcon);

        // Primary Action Buttons
        btnQuickAdd.setOnClickListener(v -> openActivity(AddBondActivity.class));
        btnViewRecommendations.setOnClickListener(v -> openActivity(AiRecommendActivity.class));

        // Header Actions
        btnNotifications.setOnClickListener(v -> openActivity(NotificationsActivity.class));
        ivMenuIcon.setOnClickListener(v -> openActivity(ProfileActivity.class));

        // Bottom Navigation Listener
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true; // Already home
            } else if (id == R.id.nav_my_bonds) {
                openActivity(MyBondsActivity.class);
                return true;
            } else if (id == R.id.nav_schedule) {
                openActivity(UpcomingDrawsActivity.class);
                return true;
            } else if (id == R.id.nav_profile) {
                openActivity(ProfileActivity.class);
                return true;
            }
            return false;
        });
    }

    private void openActivity(Class<?> cls) {
        Intent i = new Intent(this, cls);
        startActivity(i);
    }
}
