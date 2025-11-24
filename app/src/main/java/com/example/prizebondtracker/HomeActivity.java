package com.example.prizebondtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.prizebondtracker.AddBondActivity;
import com.example.prizebondtracker.AiRecommendActivity;
import com.example.prizebondtracker.MyBondsActivity;
import com.example.prizebondtracker.NotificationsActivity;
import com.example.prizebondtracker.ProfileActivity;
import com.example.prizebondtracker.UpcomingDrawsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    MaterialCardView cardAI;
    ImageButton btnNotifications;
    MaterialButton btnQuickAdd, btnViewRecommendations;
    BottomNavigationView bottomNav;
    ImageView ivMenuIcon;

    TextView tvWelcome, tvTotalBonds;

    // Placeholder Activities (You must create these empty classes)
    private static class DrawResultsActivity extends AppCompatActivity {}
    private static class WinningProbabilityActivity extends AppCompatActivity {}
    private static class PurchasingSuggestionsActivity extends AppCompatActivity {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // find views
        cardAI = findViewById(R.id.cardAI);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnQuickAdd = findViewById(R.id.btnQuickAdd);
        btnViewRecommendations = findViewById(R.id.btnViewRecommendations);
        bottomNav = findViewById(R.id.bottomNav);
        ivMenuIcon = findViewById(R.id.ivMenuIcon);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvTotalBonds = findViewById(R.id.tvTotalBonds);


        // Load Firestore data
        loadUserData();
        loadBondCount();

        // Primary Action Buttons
        btnQuickAdd.setOnClickListener(v -> openActivity(AddBondActivity.class));
        btnViewRecommendations.setOnClickListener(v -> openActivity(AiRecommendActivity.class));

        // Header Actions
        btnNotifications.setOnClickListener(v -> openActivity(NotificationsActivity.class));

        // --- UPDATED: Menu Icon Listener to show custom dropdown ---
        ivMenuIcon.setOnClickListener(this::showCustomDropdownMenu);
        // --- END UPDATED ---

        // Bottom Navigation Listener
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
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

        btnNotifications.setOnClickListener(v -> {
            Intent i = new Intent(HomeActivity.this, NotificationActivity.class);
            startActivity(i);
        });

    }

    /**
     * Displays a custom dropdown menu (PopupWindow) anchored to the menu icon.
     */
    private void showCustomDropdownMenu(View anchorView) {
        // 1. Inflate the custom layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View customMenuView = inflater.inflate(R.layout.home_menu, null);

        // 2. Create the PopupWindow
        final PopupWindow popupWindow = new PopupWindow(
                customMenuView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true // Focusable (will dismiss on outside touch)
        );

        // Optional: Set background drawable to enable outside touch dismissal (needed for focusable=true)
        // popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 3. Set click listeners for the custom menu items
        customMenuView.findViewById(R.id.menu_add_bond).setOnClickListener(v -> {
            openActivity(AddBondActivity.class);
            popupWindow.dismiss();
        });
        customMenuView.findViewById(R.id.menu_view_bonds).setOnClickListener(v -> {
            openActivity(MyBondsActivity.class);
            popupWindow.dismiss();
        });
        customMenuView.findViewById(R.id.menu_schedule).setOnClickListener(v -> {
            openActivity(UpcomingDrawsActivity.class);
            popupWindow.dismiss();
        });
        customMenuView.findViewById(R.id.menu_view_results).setOnClickListener(v -> {
            openActivity(DrawResultsActivity.class);
            popupWindow.dismiss();
        });
        customMenuView.findViewById(R.id.menu_view_probability).setOnClickListener(v -> {
            openActivity(WinningProbabilityActivity.class);
            popupWindow.dismiss();
        });
        customMenuView.findViewById(R.id.menu_purchasing_suggestions).setOnClickListener(v -> {
            openActivity(PurchasingSuggestionsActivity.class);
            popupWindow.dismiss();
        });
        customMenuView.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            performLogout();
            popupWindow.dismiss();
        });

        // 4. Show the dropdown menu anchored to the view.
        // It positions the menu just below the top-left menu icon.
        popupWindow.showAsDropDown(anchorView, 0, 16, Gravity.START);
    }

    private void performLogout() {
        // Perform Firebase logout
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();

        // You would typically redirect to LoginActivity here:
        // Intent loginIntent = new Intent(this, LoginActivity.class);
        // loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // startActivity(loginIntent);
        // finish();
    }

    private void loadUserData() {
        // NOTE: In a real app, check if getCurrentUser() is null before calling getUid()
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            tvWelcome.setText("Welcome Back, Guest");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("artifacts")
                .document("default-app-id")
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                        if (name == null || name.isEmpty()) name = "User";
                        tvWelcome.setText("Welcome Back, " + name);
                    } else {
                        tvWelcome.setText("Welcome Back, User");
                    }
                })
                .addOnFailureListener(e -> {
                    tvWelcome.setText("Welcome Back, User");
                    // Log error: Log.e("HomeActivity", "Error loading user data", e);
                });
    }

    private void loadBondCount() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            tvTotalBonds.setText("0 Bonds");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("artifacts")
                .document("default-app-id")
                .collection("users")
                .document(uid)
                .collection("bonds")
                .get()
                .addOnSuccessListener(query -> {
                    int count = query.size();
                    tvTotalBonds.setText(count + " Bonds");
                })
                .addOnFailureListener(e -> {
                    tvTotalBonds.setText("Error");
                    // Log error: Log.e("HomeActivity", "Error loading bond count", e);
                });
    }

    private void openActivity(Class<?> cls) {
        Intent i = new Intent(this, cls);
        startActivity(i);
    }
}