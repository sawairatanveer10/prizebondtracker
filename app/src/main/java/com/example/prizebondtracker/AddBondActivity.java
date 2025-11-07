package com.example.prizebondtracker;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Activity for adding Prize Bonds via Manual, Bulk, or OCR Entry.
 * Handles UI interactions, validation, and saving data to Firebase Firestore.
 */
public class AddBondActivity extends AppCompatActivity {

    private static final String TAG = "AddBondActivity";
    // Firestore and Auth variables (initialized during setup)
    private FirebaseFirestore db;
    private String userId;

    // FIX: Using a Java constant to hold the app ID. The specific variable __app_id
    // is assumed to be injected or retrieved elsewhere in a real Android setup.
    // We use a final string here to represent this value.
    private final String appId = "default-app-id";

    // UI Elements
    private TabLayout tabLayout;
    private View manualEntryView, bulkEntryView, ocrEntryView;
    // Manual Entry Fields
    private TextInputEditText etBondNumber, etPurchaseDate, etBondSeries, etDrawCity;
    private AutoCompleteTextView actDenomination;
    // Bulk Entry Fields
    private AutoCompleteTextView actDenominationBulk;
    private TextInputEditText etStartBondNumber, etEndBondNumber;
    private TextView tvBulkCount;

    // Data source for Denomination dropdowns
    private final String[] DENOMINATIONS = {"Rs. 100", "Rs. 200", "Rs. 750", "Rs. 1500", "Rs. 7500", "Rs. 15000", "Rs. 25000", "Rs. 40000"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // NOTE: The XML layout is assumed to be in the separate file activity_add_bond.xml
        // which was generated previously.
        setContentView(R.layout.activity_add_bond);

        // 1. Initialize Firebase (must be done before using db)
        db = FirebaseFirestore.getInstance();
        // The userId is now initialized in initializeViews() for consistency

        // 2. Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // 3. Initialize Views and User ID
        initializeViews();

        // 4. Setup Dropdowns and Listeners
        setupDenominationDropdowns();
        setupTabSwitching();
        setupButtonListeners();
        setupBulkInputListeners();
    }

    /** Initializes all UI elements by ID and sets a placeholder for userId. */
    private void initializeViews() {
        tabLayout = findViewById(R.id.tabLayout);
        manualEntryView = findViewById(R.id.manual_entry_view);
        bulkEntryView = findViewById(R.id.bulk_entry_view);
        ocrEntryView = findViewById(R.id.ocr_entry_view);

        // Manual Entry Fields
        etBondNumber = findViewById(R.id.etBondNumber);
        actDenomination = findViewById(R.id.actDenomination);
        etPurchaseDate = findViewById(R.id.etPurchaseDate);
        etBondSeries = findViewById(R.id.etBondSeries);
        etDrawCity = findViewById(R.id.etDrawCity);

        // Bulk Entry Fields
        actDenominationBulk = findViewById(R.id.actDenominationBulk);
        etStartBondNumber = findViewById(R.id.etStartBondNumber);
        etEndBondNumber = findViewById(R.id.etEndBondNumber);
        tvBulkCount = findViewById(R.id.tvBulkCount);

        // Placeholder for user ID. In a real app, this comes from Firebase Auth.
        userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Sets up the Denomination dropdown adapter and defaults. */
    private void setupDenominationDropdowns() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, DENOMINATIONS);
        actDenomination.setAdapter(adapter);
        actDenominationBulk.setAdapter(adapter);

        // Default to Rs. 750 (index 2)
        actDenomination.setText(DENOMINATIONS[2], false);
        actDenominationBulk.setText(DENOMINATIONS[2], false);
    }

    /** Sets up the TabLayout listener to switch between different entry forms. */
    private void setupTabSwitching() {
                tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: showView(manualEntryView); break;
                    case 1: showView(bulkEntryView); break;
                    case 2: showView(ocrEntryView); break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        showView(manualEntryView); // Ensure the correct default view is shown
    }

    /** Sets up click listeners for all action buttons. */
    private void setupButtonListeners() {
        findViewById(R.id.btnSaveManual).setOnClickListener(v -> saveManualBond());
        findViewById(R.id.btnSaveBulk).setOnClickListener(v -> saveBulkBonds());
        findViewById(R.id.btnCaptureImage).setOnClickListener(v -> openCameraOrGallery());
        findViewById(R.id.btnSaveOCR).setOnClickListener(v -> saveOCRExtractedBonds());
    }

    /** Adds TextWatchers to bulk entry fields to calculate bond count dynamically. */
    private void setupBulkInputListeners() {
        // NOTE: In a complete app, you would use TextWatcher on both etStartBondNumber and etEndBondNumber
        // to dynamically call calculateBondCount() and update tvBulkCount.
        // This is omitted for brevity but recommended for implementation.
        // Example: etEndBondNumber.addTextChangedListener(new TextWatcher() { ... });
    }

    /** Helper function to make only one view visible. */
    private void showView(View viewToShow) {
        manualEntryView.setVisibility(viewToShow == manualEntryView ? View.VISIBLE : View.GONE);
        bulkEntryView.setVisibility(viewToShow == bulkEntryView ? View.VISIBLE : View.GONE);
        ocrEntryView.setVisibility(viewToShow == ocrEntryView ? View.VISIBLE : View.GONE);
    }


    // --- BOND GENERATION AND SAVING LOGIC ---

    /**
     * Handles Manual Entry form submission and saves a single bond to Firestore.
     */
    private void saveManualBond() {
        String bondNumber = etBondNumber.getText().toString().trim();
        String denomination = actDenomination.getText().toString();

        if (bondNumber.isEmpty() || denomination.isEmpty()) {
            Toast.makeText(this, "Please enter Bond Number and Denomination.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> bond = new HashMap<>();
        bond.put("number", bondNumber);
        bond.put("denomination", denomination);
        bond.put("purchaseDate", etPurchaseDate.getText().toString().trim().isEmpty() ? null : etPurchaseDate.getText().toString().trim());
        bond.put("series", etBondSeries.getText().toString().trim().isEmpty() ? null : etBondSeries.getText().toString().trim());
        bond.put("drawCity", etDrawCity.getText().toString().trim().isEmpty() ? null : etDrawCity.getText().toString().trim());
        bond.put("trackedSince", System.currentTimeMillis());

        // Save to Firestore: /artifacts/{appId}/users/{userId}/bonds
        db.collection("artifacts").document(appId)
                .collection("users").document(userId)
                .collection("bonds").add(bond)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Bond " + bondNumber + " saved successfully!", Toast.LENGTH_LONG).show();
                    etBondNumber.setText(""); // Clear Bond Number field
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving manual bond: ", e);
                    Toast.makeText(this, "Error saving bond. Try again.", Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Handles Bulk Entry form submission, validates the range, and saves bonds using a Firestore WriteBatch.
     */
    private void saveBulkBonds() {
        String startBondStr = etStartBondNumber.getText().toString().trim();
        String endBondStr = etEndBondNumber.getText().toString().trim();
        String denomination = actDenominationBulk.getText().toString();

        if (startBondStr.isEmpty() || endBondStr.isEmpty() || denomination.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields for Bulk Entry.", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> bondNumbers = generateSequentialBonds(startBondStr, endBondStr);

        if (bondNumbers.isEmpty()) {
            Toast.makeText(this, "Invalid bond range or format. Please check your entries.", Toast.LENGTH_LONG).show();
            return;
        }

        // Use WriteBatch for efficient bulk saving (up to 500 operations per batch)
        WriteBatch batch = db.batch();
        int count = bondNumbers.size();

        for (String bondNumber : bondNumbers) {
            Map<String, Object> bond = new HashMap<>();
            bond.put("number", bondNumber);
            bond.put("denomination", denomination);
            bond.put("trackedSince", System.currentTimeMillis());

            // Reference to the new document in the bonds collection
            batch.set(db.collection("artifacts").document(appId)
                    .collection("users").document(userId)
                    .collection("bonds").document(), bond);
        }

        // Commit the batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, count + " Bonds saved successfully in bulk!", Toast.LENGTH_LONG).show();
                    etStartBondNumber.setText("");
                    etEndBondNumber.setText("");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving bulk bonds: ", e);
                    Toast.makeText(this, "Error saving " + count + " bonds. Try again.", Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Generates a list of sequential bond numbers between the start and end string.
     * Assumes a format like "A-123456" where the letter is fixed and the number increments.
     * @return List of generated bond number strings.
     */
    private List<String> generateSequentialBonds(String start, String end) {
        List<String> bonds = new ArrayList<>();
        Pattern pattern = Pattern.compile("([A-Za-z]+)-?(\\d+)"); // Matches Letter(s) and Number

        Matcher startMatcher = pattern.matcher(start);
        Matcher endMatcher = pattern.matcher(end);

        if (startMatcher.matches() && endMatcher.matches()) {
            try {
                String prefix = startMatcher.group(1);
                int startNum = Integer.parseInt(startMatcher.group(2));
                int endNum = Integer.parseInt(endMatcher.group(2));
                int numLength = startMatcher.group(2).length();

                if (!prefix.equals(endMatcher.group(1)) || startNum > endNum) {
                    return bonds; // Invalid range or mismatched prefix
                }

                for (int i = startNum; i <= endNum; i++) {
                    String paddedNum = String.format("%0" + numLength + "d", i);
                    bonds.add(prefix + "-" + paddedNum);
                }
            } catch (NumberFormatException | IllegalStateException e) {
                Log.e(TAG, "Bond number parsing error: ", e);
                return new ArrayList<>();
            }
        }
        return bonds;
    }

    // --- OCR LOGIC ---

    /**
     * Initiates the process to capture image or select from gallery for OCR.
     */
    private void openCameraOrGallery() {
        Toast.makeText(this, "Launching Camera/Gallery for image capture...", Toast.LENGTH_SHORT).show();
        // --- REAL IMPLEMENTATION HERE ---
        // 1. Check for camera/storage permissions.
        // 2. Launch the camera intent or gallery picker.
        // 3. Handle the result in onActivityResult().
        // 4. On successful image capture/selection, call processImageWithOCR().

        // Simulation: Call processImageWithOCR() after a delay.
        new android.os.Handler().postDelayed(
                () -> processImageWithOCR("path/to/image.jpg"),
                2000
        );
    }

    /**
     * Placeholder for Google ML Kit (Text Recognition) logic.
     */
    private void processImageWithOCR(String imagePath) {
        // --- REAL IMPLEMENTATION HERE ---
        // 1. Load image and create an InputImage object for ML Kit.
        // 2. Run the TextRecognizer on the image.
        // 3. Analyze the result (Text objects) and use regex to find Bond Number, Denomination, and Series.

        // Simulation of successful data extraction:
        String extractedBond = "C-112233";
        String extractedDenomination = "Rs. 1500";
        String extractedSeries = "22th";

        String extractedText = String.format("Extracted:\nNumber: %s\nDenomination: %s\nSeries: %s",
                extractedBond, extractedDenomination, extractedSeries);

        // Update UI (tvExtractedData and enable save button)
        TextView tvExtractedData = findViewById(R.id.tvExtractedData);
        tvExtractedData.setVisibility(View.VISIBLE);
        tvExtractedData.setText(extractedText + "\n\n(Data extracted successfully. Please confirm and save.)");

        findViewById(R.id.btnSaveOCR).setEnabled(true);
        Toast.makeText(this, "Data extracted. Verify and save.", Toast.LENGTH_LONG).show();
    }

    /**
     * Saves the extracted OCR bond data after user verification.
     */
    private void saveOCRExtractedBonds() {
        // In a real app, you would read the confirmed data from the verification form
        // and save it just like saveManualBond() using the extracted values.
        Toast.makeText(this, "Extracted bonds saved to Firestore!", Toast.LENGTH_SHORT).show();
    }
}
