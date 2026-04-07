package com.example.prizebondtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

public class BudgetSuggestionsActivity extends AppCompatActivity {

    private static final String APP_ID = "default-app-id";

    private EditText budgetInput;
    private Button generateButton;
    private RecyclerView suggestionsRecycler;
    private BondSuggestionAdapter adapter;
    private List<BondSuggestion> suggestionList;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_suggestions);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        budgetInput       = findViewById(R.id.budgetInput);
        generateButton    = findViewById(R.id.generateButton);
        suggestionsRecycler = findViewById(R.id.suggestionsRecycler);

        suggestionList = new ArrayList<>();
        adapter = new BondSuggestionAdapter(suggestionList);
        suggestionsRecycler.setLayoutManager(new LinearLayoutManager(this));
        suggestionsRecycler.setAdapter(adapter);

        // ── AI Insight Gate Check ────────────────────────────────────────────
        checkAiInsightEnabled();
        // ────────────────────────────────────────────────────────────────────
    }

    /**
     * Checks Firestore if user has enabled AI Insights.
     * If yes → enable UI. If no → show locked dialog.
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
                // ✅ Enabled → setup generate button normally
                setupGenerateButton();
            } else {
                // 🔒 Disabled → lock UI and show dialog
                generateButton.setEnabled(false);
                showAiInsightLockedDialog();
            }
        }).addOnFailureListener(e -> {
            generateButton.setEnabled(false);
            showAiInsightLockedDialog();
        });
    }

    private void setupGenerateButton() {
        generateButton.setOnClickListener(v -> {
            String budgetStr = budgetInput.getText().toString().trim();
            if (budgetStr.isEmpty()) {
                Toast.makeText(this, "Enter a valid budget", Toast.LENGTH_SHORT).show();
                return;
            }
            int budget = Integer.parseInt(budgetStr);
            generateDummySuggestions(budget);
        });
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
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.putExtra("open_section", "app_preferences");
                    intent.putExtra("open_section", "app_preferences");
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .show();
    }

    private void generateDummySuggestions(int budget) {
        suggestionList.clear();

        if (budget <= 2000) {
            suggestionList.add(new BondSuggestion("A", 100, budget / 100, budget));
        } else if (budget <= 5000) {
            suggestionList.add(new BondSuggestion("A", 100, 20, 2000));
            suggestionList.add(new BondSuggestion("B", 200, (budget - 2000) / 200, budget - 2000));
        } else if (budget <= 10000) {
            suggestionList.add(new BondSuggestion("A", 100, 30, 3000));
            suggestionList.add(new BondSuggestion("B", 200, 20, 4000));
            suggestionList.add(new BondSuggestion("C", 750, 2, 1500));
            suggestionList.add(new BondSuggestion("D", 1500, 1, 1500));
        } else {
            suggestionList.add(new BondSuggestion("A", 100, 50, 5000));
            suggestionList.add(new BondSuggestion("B", 200, 20, 4000));
            suggestionList.add(new BondSuggestion("C", 750, 3, 2250));
            suggestionList.add(new BondSuggestion("D", 1500, 2, 3000));
        }

        adapter.notifyDataSetChanged();
    }
}