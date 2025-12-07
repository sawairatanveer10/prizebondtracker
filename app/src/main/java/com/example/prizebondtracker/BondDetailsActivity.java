package com.example.prizebondtracker;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Locale;

public class BondDetailsActivity extends AppCompatActivity {

    private TextView tvBondNumber, tvDenomination, tvDrawsChecked, tvTimesWon, tvProbabilityPercentage, tvProbabilityInterpretation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bond_details);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize UI elements
        tvBondNumber = findViewById(R.id.tvBondNumber);
        tvDenomination = findViewById(R.id.tvDenomination);
        tvDrawsChecked = findViewById(R.id.tvDrawsChecked);
        tvTimesWon = findViewById(R.id.tvTimesWon);
        tvProbabilityPercentage = findViewById(R.id.tvProbabilityPercentage);
        tvProbabilityInterpretation = findViewById(R.id.tvProbabilityInterpretation);
        //progressBarProbability = findViewById(R.id.progressBarProbability);

        // Get bond data from Intent
        String number = getIntent().getStringExtra("bond_number");
        int denomination = getIntent().getIntExtra("denomination", 0);
        int wins = getIntent().getIntExtra("historicalWins", 0);
        int draws = getIntent().getIntExtra("drawsChecked", 0);

        // Set bond info
        tvBondNumber.setText("Bond: A-" + number);
        tvDenomination.setText("Denomination: Rs. " + denomination);
        tvDrawsChecked.setText("Draws Checked: " + draws);
        tvTimesWon.setText("Historical Wins: " + wins);

        // -------------------------
        // Probability Calculation
        // -------------------------

        // Max wins per bond = 6
        double normalizedWinRate = (double) wins / 6.0; // 0.0 to 1.0

        // Series factor: 0.5 (low) → 1.5 (high)
        double seriesFactor = 0.5 + normalizedWinRate * 1.0;

        // Denomination factor
        double denomFactor;
        switch (denomination) {
            case 100: denomFactor = 1.2; break;
            case 750: denomFactor = 1.1; break;
            case 1500: denomFactor = 1.0; break;
            case 7500: denomFactor = 0.9; break;
            case 25000: denomFactor = 0.8; break;
            default: denomFactor = 1.0; break;
        }

        // Base probability (percentage scale)
        double baseProb = 5.0;

        // Final probability
        double finalProb = baseProb * seriesFactor * denomFactor;

        // Clamp to realistic range 0.5% – 10%
        finalProb = Math.min(10.0, Math.max(0.5, finalProb));

        // Progress bar value
      //  int progress = (int) (finalProb * 10);
       //   progressBarProbability.setProgress(progress);

        // Set percentage text
        tvProbabilityPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", finalProb));

        // -------------------------
        // Probability Interpretation
        // -------------------------
        String interpretation;
        int color;

        if (finalProb >= 8.0) { // High
            interpretation = "High Probability — Strong series performance.";
            color = getColor(R.color.green);
        } else if (finalProb >= 5.0) { // Moderate
            interpretation = "Moderate Probability — Average historical behavior.";
            color = getColor(R.color.primary_green);
        } else { // Low
            interpretation = "Low Probability — Weak series historical record.";
            color = getColor(R.color.status_error);
        }

        tvProbabilityInterpretation.setText(interpretation);
        tvProbabilityInterpretation.setTextColor(color);
    }
}
