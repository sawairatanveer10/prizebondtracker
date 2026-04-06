package com.example.prizebondtracker;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class DrawResultsActivity extends AppCompatActivity {

    private Spinner spinnerDenomination, spinnerYear, spinnerDrawDate;
    private EditText etSearchNumber;
    private Button btnSearch;
    private TextView tvFirstPrize, tvSecondPrize;
    private RecyclerView rvThirdPrize;
    private ThirdPrizeAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_results);

        // Initialize views
        spinnerDenomination = findViewById(R.id.spinnerDenomination);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerDrawDate = findViewById(R.id.spinnerDrawDate);
        etSearchNumber = findViewById(R.id.etSearchNumber);
        btnSearch = findViewById(R.id.btnSearch);
        tvFirstPrize = findViewById(R.id.tvFirstPrize);
        tvSecondPrize = findViewById(R.id.tvSecondPrize);
        rvThirdPrize = findViewById(R.id.rvThirdPrize);

        // Set up the toolbar title
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);  // Set this toolbar as the ActionBar
        getSupportActionBar().setTitle("View Draw Results");  // Set the title

        // Add this for back button
        toolbar.setNavigationOnClickListener(v -> finish());

        // Firebase Firestore instance
        db = FirebaseFirestore.getInstance();

        rvThirdPrize.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ThirdPrizeAdapter(new ArrayList<>());
        rvThirdPrize.setAdapter(adapter);

        loadCategories();  // Load categories (denominations)
        setupListeners();  // Set up listeners for the spinners and search button
    }

    // Load fixed categories (denominations) into spinner
    private void loadCategories() {
        // Define fixed categories
        List<String> categories = Arrays.asList("100", "200", "750", "1500");

        // Fill spinner with fixed categories
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDenomination.setAdapter(categoryAdapter);

        // Automatically load years for the first category
        loadYears(categories.get(0));
    }

    // Load years for the selected category (denomination)
    private void loadYears(String category) {
        db.collection("draw_results")
                .whereEqualTo("category", category)  // Filter documents by selected category
                .get()  // Get the documents for the selected category
                .addOnSuccessListener(snapshot -> {
                    HashSet<String> years = new HashSet<>();
                    // Log the snapshot to check if documents are being returned
                    Log.d("DrawResults", "Documents fetched: " + snapshot.size());

                    // Loop through all documents and extract the year from the document ID
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String docId = doc.getId();  // Document ID, e.g., "100_2019_15-03-12"
                        Log.d("DrawResults", "Document ID: " + docId);  // Log the document ID

                        String[] parts = docId.split("_");  // Split the document ID by underscores
                        Log.d("DrawResults", "Parts: " + Arrays.toString(parts));  // Log the split parts

                        if (parts.length > 1) {
                            String year = parts[1];  // The year is the second part of the split document ID
                            years.add(year);  // Add the extracted year to the HashSet
                            Log.d("DrawResults", "Extracted Year: " + year);  // Log the extracted year
                        }
                    }

                    // Log the extracted years
                    Log.d("DrawResults", "Years extracted: " + years);

                    // Check if years were found
                    if (years.isEmpty()) {
                        Log.e("DrawResults", "No years found for category: " + category);
                    }

                    // Fill spinner with the extracted years
                    ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, new ArrayList<>(years));
                    yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerYear.setAdapter(yearAdapter);

                    // Automatically load draw dates for the first year (if available)
                    if (!years.isEmpty()) {
                        loadDrawDates(category, years.iterator().next());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DrawResults", "Error fetching years: " + e.getMessage());  // Log the error message
                    Toast.makeText(DrawResultsActivity.this, "Error fetching years.", Toast.LENGTH_SHORT).show();
                });
    }
    // Load draw dates for the selected year and category
    private void loadDrawDates(String category, String year) {
        db.collection("draw_results")
                .whereEqualTo("category", category)
                .whereEqualTo("year", Integer.parseInt(year))  // Filter by category and year
                .get()
                .addOnSuccessListener(snapshot -> {
                    HashSet<String> dates = new HashSet<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String[] parts = doc.getId().split("_");
                        if (parts.length > 2) {
                            dates.add(parts[2]);  // Extract date from document ID
                        }
                    }

                    // Fill spinner with draw dates
                    ArrayAdapter<String> dateAdapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, new ArrayList<>(dates));
                    dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDrawDate.setAdapter(dateAdapter);

                    if (!dates.isEmpty()) {
                        loadDrawResults(category, year, dates.iterator().next());  // Automatically load results for the first date
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DrawResultsActivity.this, "Error fetching draw dates.", Toast.LENGTH_SHORT).show();
                });
    }

    // Load draw results (first, second, third prizes) for the selected category, year, and date
    private void loadDrawResults(String category, String year, String date) {
        String docId = category + "_" + year + "_" + date;

        db.collection("draw_results")
                .document(docId)  // Fetch the specific draw result document
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("numbers")) {
                        List<String> numbers = (List<String>) doc.get("numbers");

                        // Display the results (first, second, third prizes)
                        tvFirstPrize.setText("First Prize: " + numbers.get(0));
                        if (numbers.size() >= 4) {
                            tvSecondPrize.setText("Second Prize: " + String.join(", ", numbers.subList(1, 4)));
                        }
                        if (numbers.size() > 4) {
                            adapter.updateList(numbers.subList(4, numbers.size()));
                        } else {
                            adapter.updateList(new ArrayList<>());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DrawResultsActivity.this, "Error fetching results.", Toast.LENGTH_SHORT).show();
                });
    }

    // Set up listeners for the spinners and search button
    private void setupListeners() {
        spinnerDenomination.setOnItemSelectedListener(
                new SimpleItemSelectedListener(() -> {
                    if (spinnerDenomination.getSelectedItem() != null) {
                        String category = spinnerDenomination.getSelectedItem().toString();
                        loadYears(category);
                    }
                })
        );

        spinnerYear.setOnItemSelectedListener(
                new SimpleItemSelectedListener(() -> {
                    if (spinnerDenomination.getSelectedItem() != null &&
                            spinnerYear.getSelectedItem() != null) {
                        String category = spinnerDenomination.getSelectedItem().toString();
                        String year = spinnerYear.getSelectedItem().toString();
                        loadDrawDates(category, year);
                    }
                })
        );

        spinnerDrawDate.setOnItemSelectedListener(
                new SimpleItemSelectedListener(() -> {
                    if (spinnerDenomination.getSelectedItem() != null &&
                            spinnerYear.getSelectedItem() != null &&
                            spinnerDrawDate.getSelectedItem() != null) {
                        String category = spinnerDenomination.getSelectedItem().toString();
                        String year = spinnerYear.getSelectedItem().toString();
                        String date = spinnerDrawDate.getSelectedItem().toString();
                        loadDrawResults(category, year, date);
                    }
                })
        );

        btnSearch.setOnClickListener(v -> searchNumber());
    }

    // Search the entered bond number in the displayed third prizes
    private void searchNumber() {
        String query = etSearchNumber.getText().toString().trim();
        if (TextUtils.isEmpty(query)) return;

        boolean found = adapter.highlightAndScrollTo(query);
        Toast.makeText(this,
                found ? "Number found in this draw!" : "Number not found",
                Toast.LENGTH_SHORT).show();

        hideKeyboard();
    }

    // Hide the keyboard after search
    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null && imm != null)
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }
}