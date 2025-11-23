package com.example.prizebondtracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddBondActivity extends AppCompatActivity {

    private static final String TAG = "AddBondActivity";
    private static final int REQUEST_IMAGE_PICK = 101;

    private FirebaseFirestore db;
    private String userId;
    private final String appId = "default-app-id";

    // UI
    private TabLayout tabLayout;
    private View manualEntryView, bulkEntryView, ocrEntryView;
    private TextInputEditText etBondNumber, etPurchaseDate, etBondSeries, etDrawCity,etPurchaseDateBulk,etDrawCityBulk,etBondSeriesBulk;
    private AutoCompleteTextView actDenomination, actDenominationBulk;
    private TextInputEditText etStartBondNumber, etEndBondNumber;
    private TextView tvBulkCount, tvExtractedData;
    private ImageView ivOCRPreview;

    private final String[] DENOMINATIONS = {"Rs. 100", "Rs. 200", "Rs. 750", "Rs. 1500", "Rs. 7500", "Rs. 15000", "Rs. 25000", "Rs. 40000"};

    private List<Map<String, Object>> ocrBondsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bond);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        initializeViews();
        setupDenominationDropdowns();
        setupTabSwitching();
        setupButtonListeners();
    }

    private void initializeViews() {
        tabLayout = findViewById(R.id.tabLayout);
        manualEntryView = findViewById(R.id.manual_entry_view);
        bulkEntryView = findViewById(R.id.bulk_entry_view);
        ocrEntryView = findViewById(R.id.ocr_entry_view);

        etBondNumber = findViewById(R.id.etBondNumber);
        actDenomination = findViewById(R.id.actDenomination);
        etPurchaseDate = findViewById(R.id.etPurchaseDate);
        etBondSeries = findViewById(R.id.etBondSeries);
        etDrawCity = findViewById(R.id.etDrawCity);

        actDenominationBulk = findViewById(R.id.actDenominationBulk);
        etStartBondNumber = findViewById(R.id.etStartBondNumber);
        etEndBondNumber = findViewById(R.id.etEndBondNumber);
        tvBulkCount = findViewById(R.id.tvBulkCount);

        tvExtractedData = findViewById(R.id.tvExtractedData);
        ivOCRPreview = findViewById(R.id.ivOCRPreview);

        etPurchaseDateBulk = findViewById(R.id.etPurchaseDateBulk);
        etBondSeriesBulk = findViewById(R.id.etBondSeriesBulk);
        etDrawCityBulk = findViewById(R.id.etDrawCityBulk);


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_LONG).show();
            finish();
        }
        etPurchaseDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AddBondActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                        etPurchaseDate.setText(formattedDate);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });


        etPurchaseDateBulk.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AddBondActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                        etPurchaseDateBulk.setText(formattedDate);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });


    }

    private void setupDenominationDropdowns() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, DENOMINATIONS);
        actDenomination.setAdapter(adapter);
        actDenominationBulk.setAdapter(adapter);
        actDenomination.setText(DENOMINATIONS[2], false);
        actDenominationBulk.setText(DENOMINATIONS[2], false);
    }

    private void setupTabSwitching() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: showView(manualEntryView); break;
                    case 1: showView(bulkEntryView); break;
                    case 2: showView(ocrEntryView); break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        showView(manualEntryView);
    }

    private void showView(View viewToShow) {
        manualEntryView.setVisibility(viewToShow == manualEntryView ? View.VISIBLE : View.GONE);
        bulkEntryView.setVisibility(viewToShow == bulkEntryView ? View.VISIBLE : View.GONE);
        ocrEntryView.setVisibility(viewToShow == ocrEntryView ? View.VISIBLE : View.GONE);
    }

    private void setupButtonListeners() {
        findViewById(R.id.btnSaveManual).setOnClickListener(v -> saveManualBond());
        findViewById(R.id.btnSaveBulk).setOnClickListener(v -> saveBulkBonds());
        findViewById(R.id.btnCaptureImage).setOnClickListener(v -> openImagePicker());
        findViewById(R.id.btnSaveOCR).setOnClickListener(v -> saveOCRBonds());
    }

    // --- Manual ---
    private void saveManualBond() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { showAuthError(); return; }

        String bondNumber = etBondNumber.getText().toString().trim();
        String denomination = actDenomination.getText().toString();
        if (bondNumber.isEmpty() || denomination.isEmpty()) { Toast.makeText(this,"Enter bond & denomination",Toast.LENGTH_SHORT).show(); return; }

        Map<String,Object> bond = new HashMap<>();
        bond.put("number", bondNumber);
        bond.put("denomination", denomination);
        bond.put("purchaseDate", etPurchaseDate.getText().toString().trim());
        bond.put("series", etBondSeries.getText().toString().trim());
        bond.put("drawCity", etDrawCity.getText().toString().trim());
        bond.put("trackedSince", System.currentTimeMillis());

        db.collection("artifacts").document(appId)
                .collection("users").document(user.getUid())
                .collection("bonds").add(bond)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this,"Bond saved!",Toast.LENGTH_SHORT).show();
                    etBondNumber.setText("");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG,"Error saving manual bond",e);
                    Toast.makeText(this,"Error saving bond",Toast.LENGTH_LONG).show();
                });
    }

    // --- Bulk ---
    private void saveBulkBonds() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { showAuthError(); return; }

        String start = etStartBondNumber.getText().toString().trim();
        String end = etEndBondNumber.getText().toString().trim();
        String denomination = actDenominationBulk.getText().toString();
        if (start.isEmpty() || end.isEmpty() || denomination.isEmpty()) { Toast.makeText(this,"Fill all fields",Toast.LENGTH_LONG).show(); return; }

        List<String> bonds = generateSequentialBonds(start,end);
        if (bonds.isEmpty()) { Toast.makeText(this,"Invalid range",Toast.LENGTH_LONG).show(); return; }

        WriteBatch batch = db.batch();
        for (String bn : bonds) {
            Map<String,Object> bond = new HashMap<>();
            bond.put("number", bn);
            bond.put("denomination", denomination);
            bond.put("purchaseDate", etPurchaseDateBulk.getText().toString().trim());
            bond.put("series", etBondSeriesBulk.getText().toString().trim());
            bond.put("drawCity", etDrawCityBulk.getText().toString().trim());
            bond.put("trackedSince", System.currentTimeMillis());

            batch.set(db.collection("artifacts").document(appId)
                    .collection("users").document(user.getUid())
                    .collection("bonds").document(), bond);
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this,bonds.size()+" bonds saved!",Toast.LENGTH_LONG).show();
            etStartBondNumber.setText("");
            etEndBondNumber.setText("");
        }).addOnFailureListener(e -> {
            Log.e(TAG,"Bulk save error",e);
            Toast.makeText(this,"Error saving bulk bonds",Toast.LENGTH_LONG).show();
        });
    }

    private List<String> generateSequentialBonds(String start, String end) {
        List<String> bonds = new ArrayList<>();
        Pattern p = Pattern.compile("([A-Za-z]+)-?(\\d+)");
        Matcher sm = p.matcher(start), em = p.matcher(end);
        if(sm.matches() && em.matches()){
            try{
                String prefix = sm.group(1);
                int s = Integer.parseInt(sm.group(2));
                int e = Integer.parseInt(em.group(2));
                int len = sm.group(2).length();
                if(!prefix.equals(em.group(1)) || s>e) return bonds;
                for(int i=s;i<=e;i++) bonds.add(prefix+"-"+String.format("%0"+len+"d",i));
            }catch(Exception ex){ Log.e(TAG,"Seq parse error",ex); }
        }
        return bonds;
    }

    // --- OCR ---
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQUEST_IMAGE_PICK && resultCode==RESULT_OK && data!=null){
            try {
                Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                ivOCRPreview.setImageBitmap(bitmap);
                processImageWithOCR(bitmap);
            }catch(Exception e){ Log.e(TAG,"Image pick error",e); }
        }
    }

    private void processImageWithOCR(Bitmap bitmap){
        InputImage image = InputImage.fromBitmap(bitmap,0);
        com.google.mlkit.vision.text.TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(visionText -> extractBondsFromText(visionText))
                .addOnFailureListener(e -> Toast.makeText(this,"OCR failed",Toast.LENGTH_LONG).show());
    }

    private void extractBondsFromText(Text visionText) {

        ocrBondsList.clear();

        String text = visionText.getText();

        // ---------------- BOND NUMBER PATTERN ----------------
        Pattern bondPattern = Pattern.compile("[A-Za-z]-?\\d{6}");
        Matcher mBond = bondPattern.matcher(text);

        HashSet<String> bondNumbers = new HashSet<>();

        while (mBond.find()) {
            String raw = mBond.group();

            // Normalize to A-123456
            String prefix = raw.substring(0, 1).toUpperCase();
            String digits = raw.replaceAll("[^0-9]", "");
            String finalNumber = prefix + "-" + digits;

            bondNumbers.add(finalNumber);
        }

        // ---------------- EXTRACT DENOMINATION ----------------
        String denomination = extractDenomination(text); // using helper below

        // ---------------- EXTRACT SERIES ----------------
        String series = extractSeries(text); // using helper below

        // ---------------- EXTRACT CITY ----------------
        Pattern cityPattern = Pattern.compile(
                "(RAWALPINDI|LAHORE|KARACHI|QUETTA|PESHAWAR|MULTAN|FAISALABAD|HYDERABAD)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher mCity = cityPattern.matcher(text);
        String city = mCity.find() ? mCity.group().toUpperCase() : "";

        // ---------------- BUILD FINAL RESULT ----------------
        for (String bn : bondNumbers) {

            Map<String, Object> bond = new HashMap<>();
            bond.put("number", bn);
            bond.put("denomination", denomination);
            bond.put("series", series);
            bond.put("drawCity", city);
            bond.put("purchaseDate", ""); // cannot extract from image
            bond.put("trackedSince", System.currentTimeMillis());

            ocrBondsList.add(bond);
        }

        // ---------------- DISPLAY TO USER ----------------
        if (ocrBondsList.isEmpty()) {
            tvExtractedData.setText("No bonds found");
            findViewById(R.id.btnSaveOCR).setEnabled(false);
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> b : ocrBondsList) {
                sb.append("Bond: ").append(b.get("number")).append("\n");
                sb.append("Denomination: ").append(b.get("denomination")).append("\n");
                sb.append("Series: ").append(b.get("series")).append("\n");
                sb.append("City: ").append(b.get("drawCity")).append("\n\n");
            }
            tvExtractedData.setText(sb.toString());
            findViewById(R.id.btnSaveOCR).setEnabled(true);
        }
    }

    private String extractDenomination(String text) {

        text = text.toUpperCase();

        // First: detect numeric "25000"
        if (text.contains("25000")) return "Rs. 25000";

        // Second: detect written words
        if (text.contains("TWENTY FIVE THOUSAND")) return "Rs. 25000";
        if (text.contains("FIVE THOUSAND")) return "Rs. 5000"; // for other bonds

        // Valid denominations fallback
        int[] valid = {100, 200, 750, 1500, 7500, 15000, 25000, 40000};

        Pattern p = Pattern.compile("\\d{3,5}");
        Matcher m = p.matcher(text);

        while (m.find()) {
            int num = Integer.parseInt(m.group());
            for (int v : valid) {
                if (num == v) {
                    return "Rs. " + num;
                }
            }
        }

        return "";
    }


    private String extractSeries(String text) {

        Pattern p = Pattern.compile(
                "(Series\\s*:?\\s*(\\d{1,3}))|(\\b\\d{1,3}\\b\\s*Series)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher m = p.matcher(text);

        if (m.find()) {
            return m.group().replaceAll("[^0-9]", ""); // extract only digits
        }

        return "";
    }





    private void saveOCRBonds(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user==null){ showAuthError(); return; }
        if(ocrBondsList.isEmpty()){ Toast.makeText(this,"No OCR bonds to save",Toast.LENGTH_LONG).show(); return; }

        WriteBatch batch = db.batch();
        for(Map<String,Object> bond:ocrBondsList){
            batch.set(db.collection("artifacts").document(appId)
                    .collection("users").document(user.getUid())
                    .collection("bonds").document(), bond);
        }
        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this,ocrBondsList.size()+" OCR bonds saved!",Toast.LENGTH_LONG).show();
            ocrBondsList.clear();
            tvExtractedData.setText("");
            ivOCRPreview.setImageDrawable(null);
        }).addOnFailureListener(e -> {
            Log.e(TAG,"OCR save error",e);
            Toast.makeText(this,"Error saving OCR bonds",Toast.LENGTH_LONG).show();
        });
    }

    private void showAuthError(){ Toast.makeText(this,"User not authenticated",Toast.LENGTH_LONG).show(); }

}
