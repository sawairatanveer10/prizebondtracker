package com.example.prizebondtracker;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DrawResultsActivity extends AppCompatActivity {

    private Spinner spinnerYear, spinnerDenomination, spinnerDrawDate;
    private EditText etSearchNumber;
    private Button btnSearch, btnViewPDF;
    private TextView tvFirstPrize, tvSecondPrize;
    private RecyclerView rvThirdPrize;
    private ThirdPrizeAdapter adapter;

    private final HashMap<String, HashMap<String, HashMap<String, DrawData>>> dummyDB = new HashMap<>();
    private final Map<String, List<String>> yearDraws = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_results);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("View Draw Results");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Views
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerDenomination = findViewById(R.id.spinnerDenomination);
        spinnerDrawDate = findViewById(R.id.spinnerDrawDate);
        etSearchNumber = findViewById(R.id.etSearchNumber);
        btnSearch = findViewById(R.id.btnSearch);
        btnViewPDF = findViewById(R.id.btnViewPDF);
        tvFirstPrize = findViewById(R.id.tvFirstPrize);
        tvSecondPrize = findViewById(R.id.tvSecondPrize);
        rvThirdPrize = findViewById(R.id.rvThirdPrize);

        setupYearDraws();
        setupDummyData();
        setupSpinners();
        setupRecycler();

        spinnerYear.setOnItemSelectedListener(new SimpleItemSelectedListener(this::refreshDrawDates));
        spinnerDenomination.setOnItemSelectedListener(new SimpleItemSelectedListener(this::refreshDrawDates));
        spinnerDrawDate.setOnItemSelectedListener(new SimpleItemSelectedListener(this::refreshPrizes));

        btnViewPDF.setOnClickListener(v -> {
            String year = spinnerYear.getSelectedItem().toString();
            String denom = spinnerDenomination.getSelectedItem().toString();
            String draw = spinnerDrawDate.getSelectedItem() != null ? spinnerDrawDate.getSelectedItem().toString() : "";
            Toast.makeText(this, "Open PDF (dummy) for: " + year + " - " + denom + " - " + draw, Toast.LENGTH_SHORT).show();
        });

        btnSearch.setOnClickListener(v -> {
            String q = etSearchNumber.getText().toString().trim();
            hideKeyboard();
            if (TextUtils.isEmpty(q)) {
                Toast.makeText(this, "Enter a number to search", Toast.LENGTH_SHORT).show();
                return;
            }

            String year = spinnerYear.getSelectedItem().toString();
            String denom = spinnerDenomination.getSelectedItem().toString();
            String draw = spinnerDrawDate.getSelectedItem() != null ? spinnerDrawDate.getSelectedItem().toString() : "";

            DrawData sel = null;
            if (dummyDB.containsKey(year) && dummyDB.get(year).containsKey(denom)) {
                sel = dummyDB.get(year).get(denom).get(draw);
            }

            if (sel == null) {
                Toast.makeText(this, "No draw data available", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean found = false;

            // First prize
            if (q.equals(sel.firstPrize)) found = true;

            // Second prize
            if (!found && sel.secondPrizes.contains(q)) found = true;

            // Third prize
            if (!found && sel.thirdPrizes.contains(q)) {
                found = true;
                adapter.highlightAndScrollTo(q); // Highlight in RecyclerView
            }

            if (found) {
                Toast.makeText(this, "Number found in this draw!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Number not found in this draw", Toast.LENGTH_SHORT).show();
            }
        });

        // Initial populate
        refreshDrawDates();
    }

    private void setupYearDraws() {
        yearDraws.put("2025", Arrays.asList("15-Jan-2025","15-Apr-2025","15-Jul-2025","15-Oct-2025"));
        yearDraws.put("2024", Arrays.asList("15-Jan-2024","15-Apr-2024","15-Jul-2024","15-Oct-2024"));
        yearDraws.put("2023", Arrays.asList("15-Jan-2023","15-Apr-2023","15-Jul-2023","15-Oct-2023"));
    }

    private void setupDummyData() {
        String[] years = {"2025","2024","2023"};
        String[] denoms = {"100","200","750","1500","25000","40000"};
        Random rnd = new Random();

        for (String year : years) {
            for (String denom : denoms) {
                List<String> draws = yearDraws.get(year);
                for (String draw : draws) {
                    // First prize: 1 bond
                    String firstPrize = String.format("%06d", rnd.nextInt(900000) + 100000);

                    // Second prize: 3 bonds
                    List<String> secondPrizes = new ArrayList<>();
                    while (secondPrizes.size() < 3) {
                        String num = String.format("%06d", rnd.nextInt(900000) + 100000);
                        if (!num.equals(firstPrize)) secondPrizes.add(num); // avoid duplicates
                    }

                    // Third prize: 9–10 bonds
                    List<String> thirdPrizes = new ArrayList<>();
                    int thirdCount = rnd.nextBoolean() ? 9 : 10;
                    while (thirdPrizes.size() < thirdCount) {
                        String num = String.format("%06d", rnd.nextInt(900000) + 100000);
                        if (!num.equals(firstPrize) && !secondPrizes.contains(num) && !thirdPrizes.contains(num)) {
                            thirdPrizes.add(num);
                        }
                    }

                    DrawData d = new DrawData(firstPrize, secondPrizes, thirdPrizes);
                    putDummy(year, denom, draw, d);
                }
            }
        }
    }

    private void putDummy(String year, String denom, String date, DrawData data){
        dummyDB.putIfAbsent(year, new HashMap<>());
        HashMap<String, HashMap<String, DrawData>> byYear = dummyDB.get(year);
        byYear.putIfAbsent(denom, new HashMap<>());
        HashMap<String, DrawData> byDenom = byYear.get(denom);
        byDenom.put(date, data);
    }

    private void setupSpinners(){
        // Year Spinner
        List<String> years = new ArrayList<>(yearDraws.keySet());
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        // Denominations spinner
        List<String> denoms = Arrays.asList("100","200","750","1500","25000","40000");
        ArrayAdapter<String> denomAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, denoms);
        denomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDenomination.setAdapter(denomAdapter);
    }

    private void refreshDrawDates(){
        String year = spinnerYear.getSelectedItem().toString();
        String denom = spinnerDenomination.getSelectedItem().toString();
        List<String> dates = new ArrayList<>();
        if (dummyDB.containsKey(year) && dummyDB.get(year).containsKey(denom)) {
            dates.addAll(dummyDB.get(year).get(denom).keySet());
        }
        ArrayAdapter<String> dateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dates);
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDrawDate.setAdapter(dateAdapter);
        refreshPrizes();
    }

    private void refreshPrizes(){
        String year = spinnerYear.getSelectedItem().toString();
        String denom = spinnerDenomination.getSelectedItem().toString();
        String draw = spinnerDrawDate.getSelectedItem() != null ? spinnerDrawDate.getSelectedItem().toString() : "";

        DrawData sel = null;
        if (dummyDB.containsKey(year) && dummyDB.get(year).containsKey(denom)) {
            sel = dummyDB.get(year).get(denom).get(draw);
        }

        if (sel == null) {
            tvFirstPrize.setText("First Prize: —");
            tvSecondPrize.setText("Second Prize: —");
            adapter.updateList(new ArrayList<>());
            return;
        }

        tvFirstPrize.setText("First Prize: " + sel.firstPrize);
        tvSecondPrize.setText("Second Prize: " + TextUtils.join(", ", sel.secondPrizes));
        adapter.updateList(new ArrayList<>(sel.thirdPrizes));
    }

    private void setupRecycler(){
        rvThirdPrize.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ThirdPrizeAdapter(new ArrayList<>());
        rvThirdPrize.setAdapter(adapter);
    }

    private void hideKeyboard(){
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if(getCurrentFocus()!=null && imm!=null) imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    private static class DrawData {
        String firstPrize;
        List<String> secondPrizes;
        List<String> thirdPrizes;
        DrawData(String f, List<String> s, List<String> t){
            this.firstPrize = f;
            this.secondPrizes = s;
            this.thirdPrizes = t;
        }
    }
}
