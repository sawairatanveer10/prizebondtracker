package com.example.prizebondtracker;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WinningProbabilityActivity extends AppCompatActivity {

    private MaterialAutoCompleteTextView dropdownBondSelector;
    private TextView tvSelectedBondDetails;
    private TextView tvProbabilityPercentage;
    private TextView tvProbabilityInterpretation;
    private TextView tvDrawsChecked;
    private TextView tvTimesWon;
    private ProgressBar progressBarProbability;

    // Bond Model
    private static class Bond {
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

        @Override
        public String toString() {
            return String.format(Locale.getDefault(), "%s (Rs. %d)", number, denomination);
        }
    }

    private List<Bond> registeredBonds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_winning_probability);

        // Toolbar Setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // UI Initialization
        dropdownBondSelector = findViewById(R.id.dropdownBondSelector);
        tvSelectedBondDetails = findViewById(R.id.tvSelectedBondDetails);
        tvProbabilityPercentage = findViewById(R.id.tvProbabilityPercentage);
        tvProbabilityInterpretation = findViewById(R.id.tvProbabilityInterpretation);
        tvDrawsChecked = findViewById(R.id.tvDrawsChecked);
        tvTimesWon = findViewById(R.id.tvTimesWon);
        progressBarProbability = findViewById(R.id.progressBarProbability);

        // Load Bond Data
        loadDummyBonds();

        // Setup Dropdown Adapter
        ArrayAdapter<Bond> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,  // Visible white dropdown items
                registeredBonds
        );

        dropdownBondSelector.setAdapter(adapter);

        // Dropdown item selection
        dropdownBondSelector.setOnItemClickListener((parent, view, position, id) -> {
            Bond selected = registeredBonds.get(position);
            calculateAndDisplayProbability(selected);
        });

        // Load first bond by default
        if (!registeredBonds.isEmpty()) {
            dropdownBondSelector.setText(registeredBonds.get(0).toString(), false);
            calculateAndDisplayProbability(registeredBonds.get(0));
        }
    }

    // Dummy Bond Data
    private void loadDummyBonds() {
        registeredBonds = new ArrayList<>();
        registeredBonds.add(new Bond("205001", 1500, 3, 160));
        registeredBonds.add(new Bond("019945", 750, 1, 160));
        registeredBonds.add(new Bond("777111", 100, 6, 160));
        registeredBonds.add(new Bond("000002", 25000, 0, 160));
    }

    // Probability Logic
    private void calculateAndDisplayProbability(Bond bond) {

        // -----------------------------
        // 1️⃣ BASE PROBABILITY (Neutral)
        // -----------------------------
        // Represents a small normalized chance (real world neutral)
        double baseProb = 1.0;   // neutral baseline


        // -----------------------------------------
        // 2️⃣ SERIES PERFORMANCE FACTOR (Key Logic)
        // -----------------------------------------
        // How strong the bond's SERIES performed historically
        double seriesWinRate = (double) bond.historicalWins / bond.drawsChecked;

        double seriesFactor;
        if (seriesWinRate >= 0.06) {          // 6%+
            seriesFactor = 1.5;               // very strong series
        } else if (seriesWinRate >= 0.04) {   // 4%-6%
            seriesFactor = 1.3;               // strong series
        } else if (seriesWinRate >= 0.02) {   // 2%-4%
            seriesFactor = 1.1;               // decent
        } else {
            seriesFactor = 0.8;               // weak series
        }


        // ----------------------------------------------
        // 3️⃣ DENOMINATION FACTOR (Difficulty Adjustment)
        // ----------------------------------------------
        double denomFactor;

        if (bond.denomination == 100) {
            denomFactor = 1.2;
        } else if (bond.denomination == 750) {
            denomFactor = 1.1;
        } else if (bond.denomination == 1500) {
            denomFactor = 1.0;
        } else if (bond.denomination == 7500) {
            denomFactor = 0.9;
        } else if (bond.denomination == 25000) {
            denomFactor = 0.8;
        } else {
            denomFactor = 1.0;  // default safe value
        }


        // -----------------------------
        // 🧮 FINAL ML-READY FORMULA
        // -----------------------------
        double finalProbability = baseProb * seriesFactor * denomFactor;

        // Limit display between 0% and 10%
        finalProbability = Math.min(10.0, Math.max(0.5, finalProbability));

        // Convert for progress bar
        int progressValue = (int) (finalProbability * 10);

        // -----------------------------
        // UPDATE UI COMPONENTS
        // -----------------------------

        // Bond details
        tvSelectedBondDetails.setText(
                String.format(Locale.getDefault(),
                        "Bond # %s | Denomination: Rs. %d",
                        bond.number, bond.denomination)
        );

        // Historical stats
        tvDrawsChecked.setText(String.valueOf(bond.drawsChecked));
        tvTimesWon.setText(String.valueOf(bond.historicalWins));

        // Progress Bar + % value
        progressBarProbability.setProgress(progressValue);
        tvProbabilityPercentage.setText(
                String.format(Locale.getDefault(), "%.1f%%", finalProbability)
        );

        // -----------------------------
        // INTERPRETATION TEXT
        // -----------------------------
        String interpretation;
        int color;

        if (finalProbability >= 7.0) {
            interpretation = "High Probability — Strong series performance.";
            color = getColor(R.color.green);

        } else if (finalProbability >= 4.0) {
            interpretation = "Moderate Probability — Average historical behavior.";
            color = getColor(R.color.primary_green);

        } else {
            interpretation = "Low Probability — Weak series historical record.";
            color = getColor(R.color.status_error);
        }

        tvProbabilityInterpretation.setText(interpretation);
        tvProbabilityInterpretation.setTextColor(color);
    }

}
