package com.example.prizebondtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class WinningProbabilityActivity extends AppCompatActivity {

    private static final String APP_ID = "default-app-id";

    private RecyclerView recyclerBonds;
    private ProgressBar progressBar; // optional loading indicator
    private List<Bond> registeredBonds;

    public static class Bond {
        String number;
        int denomination;
        int historicalWins;
        int drawsChecked;

        public Bond(String number, int denomination, int historicalWins, int drawsChecked) {
            this.number = number;
            this.denomination = denomination;
            this.historicalWins = historicalWins;
            this.drawsChecked = drawsChecked;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_winning_probability);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerBonds = findViewById(R.id.recyclerBonds);
        recyclerBonds.setLayoutManager(new LinearLayoutManager(this));

        // ── AI Insight Gate Check ────────────────────────────────────────────
        checkAiInsightEnabled();
        // ────────────────────────────────────────────────────────────────────
    }

    /**
     * Checks Firestore if user has enabled AI Insights.
     * If yes → load content. If no → show locked dialog.
     */
    private void checkAiInsightEnabled() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        String userId = currentUser.getUid();
        DocumentReference userDocRef = FirebaseFirestore.getInstance()
                .collection("artifacts")
                .document(APP_ID)
                .collection("users")
                .document(userId);

        userDocRef.get().addOnSuccessListener(doc -> {
            Boolean aiEnabled = doc.getBoolean("enable_ai_insight");

            if (aiEnabled != null && aiEnabled) {
                // ✅ Preference ON → load page normally
                loadPageContent();
            } else {
                // 🔒 Preference OFF → show locked message
                showAiInsightLockedDialog();
            }
        }).addOnFailureListener(e -> {
            // On error, show locked as safe fallback
            showAiInsightLockedDialog();
        });
    }

    private void loadPageContent() {
        loadDummyBonds();
        BondProbabilityAdapter adapter = new BondProbabilityAdapter(this, registeredBonds);
        recyclerBonds.setAdapter(adapter);
    }

    /**
     * Dialog shown when enable_ai_insight is false.
     * Contains a link to Profile → App Preferences.
     */
    private void showAiInsightLockedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("AI Insights Disabled")
                .setMessage("This feature requires AI Insights to be enabled.\n\n"
                        + "Please go to your Profile → App Preferences and turn on \"Enable AI Insights\" to use this page.")
                .setCancelable(false)
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    // Navigate to ProfileActivity (your fragment host)
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.putExtra("open_section", "app_preferences");
                    // If ProfileActivity is a Fragment inside an Activity,
                    // replace ProfileActivity_Host with the correct host Activity name
                    intent.putExtra("open_section", "app_preferences");
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .show();
    }

    private void loadDummyBonds() {
        registeredBonds = new ArrayList<>();
        registeredBonds.add(new Bond("205001", 1500, 6, 160));
        registeredBonds.add(new Bond("123456", 1500, 4, 160));
        registeredBonds.add(new Bond("777111", 100,  1, 160));
        registeredBonds.add(new Bond("019945", 750,  3, 160));
        registeredBonds.add(new Bond("000002", 25000,0, 160));
    }
}