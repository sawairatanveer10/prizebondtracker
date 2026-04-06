package com.example.prizebondtracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.android.volley.DefaultRetryPolicy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddBondActivity extends AppCompatActivity {

    // OCR ke liye — existing fields ke neeche add karein
    private Uri cameraImageUri;
    private static final int REQUEST_IMAGE_CAPTURE = 201;
    private static final int REQUEST_IMAGE_PICK = 202;
    private FirebaseFirestore db;
    private String userId;
    private final String appId = "default-app-id";

    private TabLayout tabLayout;
    private TextInputEditText etBondNumber, etPurchaseDate, etBondSeries;
    private TextInputEditText etStartBondNumber, etEndBondNumber;
    private TextInputEditText etPurchaseDateBulk, etBondSeriesBulk;
    private AutoCompleteTextView etDrawCity, etDrawCityBulk;
    private AutoCompleteTextView actDenomination, actDenominationBulk;
    private ImageView ivOCRPreview;
    private TextView tvExtractedData;
    private MaterialButton btnSaveOCR; // add this


    private final String[] DENOMINATIONS = {
            "Rs. 100","Rs. 200","Rs. 750","Rs. 1500"
                };

    private List<Map<String,Object>> ocrBondsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bond);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null){
            Toast.makeText(this,"Please login first",Toast.LENGTH_LONG).show();
            finish(); return;
        }
        userId = user.getUid();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());


        initializeViews();

        setupCityAutoComplete(etDrawCity);
        setupCityAutoComplete(etDrawCityBulk);

        setupDenominationDropdowns();
        setupTabSwitching();
        setupButtons();
    }

    private void initializeViews(){
        tabLayout = findViewById(R.id.tabLayout);

        etBondNumber = findViewById(R.id.etBondNumber);
        etPurchaseDate = findViewById(R.id.etPurchaseDate);
        etBondSeries = findViewById(R.id.etBondSeries);
        etDrawCity = findViewById(R.id.etDrawCity);

        etStartBondNumber = findViewById(R.id.etStartBondNumber);
        etEndBondNumber = findViewById(R.id.etEndBondNumber);
        etPurchaseDateBulk = findViewById(R.id.etPurchaseDateBulk);
        etBondSeriesBulk = findViewById(R.id.etBondSeriesBulk);
        etDrawCityBulk = findViewById(R.id.etDrawCityBulk);

        actDenomination = findViewById(R.id.actDenomination);
        actDenominationBulk = findViewById(R.id.actDenominationBulk);

        ivOCRPreview = findViewById(R.id.ivOCRPreview);
        tvExtractedData = findViewById(R.id.tvExtractedData);

        btnSaveOCR = findViewById(R.id.btnSaveOCR);
        btnSaveOCR.setEnabled(false); // initially disabled


        setupDatePicker(etPurchaseDate);
        setupDatePicker(etPurchaseDateBulk);
    }

    private void setupDatePicker(TextInputEditText field){
        field.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this,(view,y,m,d)->
                    field.setText(String.format("%02d/%02d/%04d",d,m+1,y)),
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void setupDenominationDropdowns(){
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,DENOMINATIONS);
        actDenomination.setAdapter(adapter);
        actDenominationBulk.setAdapter(adapter);
        actDenomination.setText(DENOMINATIONS[2],false);
        actDenominationBulk.setText(DENOMINATIONS[2],false);
    }

    private void setupTabSwitching(){
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { showView(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        showView(0);
    }

    private void showView(int index){
        findViewById(R.id.manual_entry_view).setVisibility(index==0? View.VISIBLE:View.GONE);
        findViewById(R.id.bulk_entry_view).setVisibility(index==1?View.VISIBLE:View.GONE);
        findViewById(R.id.ocr_entry_view).setVisibility(index==2?View.VISIBLE:View.GONE);
    }

    private void setupButtons(){
        findViewById(R.id.btnSaveManual).setOnClickListener(v -> handleManualSave());
        findViewById(R.id.btnSaveBulk).setOnClickListener(v -> saveBulkBonds());
        findViewById(R.id.btnCaptureImage).setOnClickListener(v -> pickImage());
        findViewById(R.id.btnSaveOCR).setOnClickListener(v -> saveOCRBonds());
    }

    private boolean isValidBondNumber(String bond){
        return bond.matches("^[A-Z]-\\d{6}$");
    }

    private void showError(TextInputEditText field,String msg){
        field.setError(msg); field.requestFocus();
    }

    // ========== MANUAL ENTRY ==========
    private void handleManualSave(){
        String bondNumber = etBondNumber.getText().toString().trim().toUpperCase();
        String denomination = actDenomination.getText().toString().trim();

        if(bondNumber.isEmpty()){ showError(etBondNumber,"Bond number required"); return; }
        if(!isValidBondNumber(bondNumber)){ showError(etBondNumber,"Format A-123456"); return; }
        if(denomination.isEmpty()){ Toast.makeText(this,"Denomination required",Toast.LENGTH_SHORT).show(); return; }
        if (etDrawCity.getAdapter() == null ||
                ((ArrayAdapter) etDrawCity.getAdapter()).getPosition(etDrawCity.getText().toString()) == -1) {
            etDrawCity.setError("Select valid Pakistani city");
            return;
        }
        String series = etBondSeries.getText().toString().trim().toUpperCase();

        if (!series.isEmpty() && !series.matches("^[A-Z]$")) {
            showError(etBondSeries, "Only 1 alphabet allowed (A-Z)");
            return;
        }
        // Duplicate check for same user
        db.collection("artifacts").document(appId)
                .collection("users").document(userId)
                .collection("bonds").whereEqualTo("number",bondNumber)
                .get().addOnSuccessListener(snap->{
                    if(!snap.isEmpty()){
                        Toast.makeText(this,"Bond already stored by you",Toast.LENGTH_LONG).show();
                    } else {
                        saveManualBond(bondNumber,denomination);
                    }
                });
    }

    private void saveManualBond(String bondNumber,String denomination){
        Map<String,Object> bond = new HashMap<>();
        bond.put("number",bondNumber);
        bond.put("denomination",denomination);
        bond.put("purchaseDate",etPurchaseDate.getText().toString().trim());
        bond.put("series",etBondSeries.getText().toString().trim());
        bond.put("drawCity",etDrawCity.getText().toString().trim());
        bond.put("trackedSince",System.currentTimeMillis());

        db.collection("artifacts").document(appId)
                .collection("users").document(userId)
                .collection("bonds")
                .add(bond)
                .addOnSuccessListener(d-> Toast.makeText(this,"Bond saved",Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e-> Toast.makeText(this,"Error saving bond",Toast.LENGTH_LONG).show());
    }

    // ========== BULK ENTRY ==========
    private void saveBulkBonds() {
        String start = etStartBondNumber.getText().toString().trim().toUpperCase();
        String end = etEndBondNumber.getText().toString().trim().toUpperCase();
        String denomination = actDenominationBulk.getText().toString().trim();

        if (start.isEmpty() || end.isEmpty() || denomination.isEmpty()) {
            Toast.makeText(this, "All * fields required", Toast.LENGTH_LONG).show();
            return;
        }

        // Validate bond format
        if (!isValidBondNumber(start)) {
            showError(etStartBondNumber, "Format must be A-123456");
            return;
        }
        if (!isValidBondNumber(end)) {
            showError(etEndBondNumber, "Format must be A-123456");
            return;
        }
        if (etDrawCityBulk.getAdapter() == null ||
                ((ArrayAdapter) etDrawCityBulk.getAdapter()).getPosition(etDrawCityBulk.getText().toString()) == -1) {
            etDrawCityBulk.setError("Select valid Pakistani city");
            return;
        }

        // Validate series matching
        char startSeries = start.charAt(0);
        char endSeries = end.charAt(0);
        if (startSeries != endSeries) {
            showError(etEndBondNumber, "Start and End bond series must match");
            return;
        }

        List<String> bonds = generateSequentialBonds(start, end);
        if (bonds.isEmpty()) {
            Toast.makeText(this, "Invalid range", Toast.LENGTH_LONG).show();
            return;
        }
        String seriesBulk = etBondSeriesBulk.getText().toString().trim().toUpperCase();

        if (!seriesBulk.isEmpty() && !seriesBulk.matches("^[A-Z]$")) {
            showError(etBondSeriesBulk, "Only 1 alphabet allowed (A-Z)");
            return;
        }

        // Check for duplicates first
        db.collection("artifacts").document(appId)
                .collection("users").document(userId)
                .collection("bonds")
                .whereIn("number", bonds)
                .get()
                .addOnSuccessListener(snapshot -> {
                    HashSet<String> existing = new HashSet<>();
                    for (var doc : snapshot.getDocuments()) {
                        existing.add(doc.getString("number"));
                    }

                    List<String> toSaveBonds = new ArrayList<>();
                    for (String bn : bonds) {
                        if (!existing.contains(bn)) {
                            toSaveBonds.add(bn);
                        }
                    }

                    if (toSaveBonds.isEmpty()) {
                        Toast.makeText(this, "All bonds in this range already exist", Toast.LENGTH_LONG).show();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (String bn : toSaveBonds) {
                        Map<String, Object> bond = new HashMap<>();
                        bond.put("number", bn);
                        bond.put("denomination", denomination);
                        bond.put("purchaseDate", etPurchaseDateBulk.getText().toString().trim());
                        bond.put("series", String.valueOf(startSeries));
                        bond.put("drawCity", etDrawCityBulk.getText().toString().trim());
                        bond.put("trackedSince", System.currentTimeMillis());

                        batch.set(db.collection("artifacts").document(appId)
                                .collection("users").document(userId)
                                .collection("bonds").document(bn), bond);
                    }

                    batch.commit().addOnSuccessListener(v ->
                                    Toast.makeText(this, toSaveBonds.size() + " bonds saved", Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error saving bonds", Toast.LENGTH_LONG).show());
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Error checking duplicates", Toast.LENGTH_LONG).show());
    }


    private List<String> generateSequentialBonds(String start,String end){
        List<String> list = new ArrayList<>();
        Pattern p = Pattern.compile("^([A-Z])-(\\d{6})$");
        Matcher sm = p.matcher(start);
        Matcher em = p.matcher(end);
        if(!sm.matches()||!em.matches()) return list;

        int s=Integer.parseInt(sm.group(2)), e=Integer.parseInt(em.group(2));
        if(e<s) return list;
        for(int i=s;i<=e;i++) list.add(sm.group(1)+"-"+String.format("%06d",i));
        return list;
    }

    // ========== OCR ENTRY ==========
    private void pickImage() {
        String[] options = {"Camera", "Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Full resolution camera via FileProvider
                        File photoFile = new File(
                                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                "bond_ocr_" + System.currentTimeMillis() + ".jpg"
                        );
                        cameraImageUri = FileProvider.getUriForFile(
                                this,
                                getPackageName() + ".fileprovider",
                                photoFile
                        );
                        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        takePicture.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                        startActivityForResult(takePicture, REQUEST_IMAGE_CAPTURE);
                    } else {
                        Intent pickPhoto = new Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        );
                        pickPhoto.setType("image/*");
                        startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK);
                    }
                }).show();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        Uri imageUri = null;

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            // FileProvider se full resolution URI seedha milti hai
            imageUri = cameraImageUri;
        } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
            imageUri = data.getData();
        }

        if (imageUri != null) {
            try {
                // Full resolution bitmap load karein
                Bitmap bitmap = MediaStore.Images.Media
                        .getBitmap(getContentResolver(), imageUri);
                ivOCRPreview.setImageBitmap(bitmap);
                tvExtractedData.setText("Scanning image...");
                processOCR(bitmap);
            } catch (IOException e) {
                Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void processOCR(Bitmap bitmap){
        InputImage image = InputImage.fromBitmap(bitmap,0);
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener(this::extractBondsFromText)
                .addOnFailureListener(e-> Toast.makeText(this,"OCR failed",Toast.LENGTH_LONG).show());
    }

    private void extractBondsFromText(Text visionText) {
        ocrBondsList.clear();
        String rawText = visionText.getText();

        // ---- Bond number extraction ----
        // OCR aksar "A 123456", "A-123456", "A123456" return karta hai
        // Yeh pattern teeno handle karta hai
        Pattern bondPattern = Pattern.compile(
                "\\b([A-Z])[-\\s]?(\\d{6})\\b"
        );
        Matcher mBond = bondPattern.matcher(rawText.toUpperCase());
        HashSet<String> bondNumbers = new HashSet<>();
        while (mBond.find()) {
            // Standard format mein normalize karein: A-123456
            String normalized = mBond.group(1) + "-" + mBond.group(2);
            bondNumbers.add(normalized);
        }

        // ---- Denomination extraction ----
        String denomination = extractDenomination(rawText);

        // ---- Series extraction ----
        String series = extractSeries(rawText);

        // ---- Results build karein ----
        if (bondNumbers.isEmpty()) {
            tvExtractedData.setText(
                    "No bond numbers found.\n\nScanned text:\n" + rawText
            );
            btnSaveOCR.setEnabled(false);
            return;
        }

        if (denomination.isEmpty()) {
            // User ko manually select karne do — professional behavior
            showDenominationDialog(bondNumbers, series);
            return;
        }
        for (String bn : bondNumbers) {
            Map<String, Object> bond = new HashMap<>();
            bond.put("number", bn);
            bond.put("denomination", denomination);
            bond.put("series", series);
            bond.put("drawCity", "");
            bond.put("purchaseDate", "");
            bond.put("trackedSince", System.currentTimeMillis());
            ocrBondsList.add(bond);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("✓ Found ").append(ocrBondsList.size()).append(" bond(s):\n\n");
        for (Map<String, Object> b : ocrBondsList) {
            sb.append("Bond:         ").append(b.get("number")).append("\n");
            sb.append("Denomination: ").append(b.get("denomination")).append("\n");
            if (!b.get("series").toString().isEmpty()) {
                sb.append("Series:       ").append(b.get("series")).append("\n");
            }
            sb.append("\n");
        }
        tvExtractedData.setText(sb.toString());
        btnSaveOCR.setEnabled(true);
    }

    private String extractDenomination(String text) {
        // Step 1: normalize — newlines ko space se replace karo
        // ML Kit aksar "Rs.\n25000" ya "Rs. \n 25,000" return karta hai
        String cleaned = text
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s{2,}", " ")  // double spaces hatao
                .toUpperCase();

        // Step 2: "Rs." ke baad wala number directly dhundo
        // Yeh pattern "Rs. 25000", "Rs.25000", "Rs 25000" sab handle karta hai
        Pattern rsPattern = Pattern.compile(
                "RS\\.?\\s*(\\d[\\d,]+)"
        );
        Matcher rsMatcher = rsPattern.matcher(cleaned);
        if (rsMatcher.find()) {
            String num = rsMatcher.group(1).replace(",", ""); // "25,000" → "25000"
            String mapped = mapNumberToDenomination(num);
            if (!mapped.isEmpty()) return mapped;
        }

        // Step 3: fallback — plain number dhundo (agar Rs. missing ho)
        String[][] aliases = {
                {"Rs. 100",   "100"},
                {"Rs. 200",   "200"},
                {"Rs. 750",   "750",   "7S0"},
                {"Rs. 1500",  "1500",  "1500"},
                {"Rs. 7500",  "7500"},
                {"Rs. 15000", "15000", "15000"},
                {"Rs. 25000", "25000", "25000"},
                {"Rs. 40000", "40000", "40000"},
        };

        // Bada number pehle check karo (25000 se shuru) — "100" "25000" ke andar match na ho
        for (int i = aliases.length - 1; i >= 0; i--) {
            for (int j = 1; j < aliases[i].length; j++) {
                Pattern p = Pattern.compile(
                        "(?<![\\d])" + Pattern.quote(aliases[i][j]) + "(?![\\d])"
                );
                if (p.matcher(cleaned).find()) {
                    return aliases[i][0];
                }
            }
        }
        return "";
    }

    private String mapNumberToDenomination(String num) {
        switch (num.trim()) {
            case "100":   return "Rs. 100";
            case "200":   return "Rs. 200";
            case "750":   return "Rs. 750";
            case "1500":  return "Rs. 1500";
            case "7500":  return "Rs. 7500";
            case "15000": return "Rs. 15000";
            case "25000": return "Rs. 25000";
            case "40000": return "Rs. 40000";
            default:      return "";
        }
    }
    private String extractSeries(String text) {
        // Prize bond series formats:
        // "Series: 52", "52nd Series", "SERIES NO 52", "S-52"
        Pattern[] patterns = {
                Pattern.compile(
                        "(?:series|ser)[\\s.:/-]*no[\\s.:/-]*(\\d{1,3})",
                        Pattern.CASE_INSENSITIVE
                ),
                Pattern.compile(
                        "(?:series|ser)[\\s.:/-]*(\\d{1,3})",
                        Pattern.CASE_INSENSITIVE
                ),
                Pattern.compile(
                        "(\\d{1,3})(?:st|nd|rd|th)?\\s*series",
                        Pattern.CASE_INSENSITIVE
                ),
                Pattern.compile(
                        "S[-\\s](\\d{1,3})\\b",
                        Pattern.CASE_INSENSITIVE
                ),
        };

        for (Pattern p : patterns) {
            Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group(1);
            }
        }
        return "";
    }

    private void saveOCRBonds() {
        if (ocrBondsList.isEmpty()) {
            Toast.makeText(this, "No OCR bonds", Toast.LENGTH_LONG).show();
            return;
        }

        // Fetch existing bonds first
        List<String> bondNumbers = new ArrayList<>();
        for (Map<String, Object> b : ocrBondsList) {
            bondNumbers.add((String) b.get("number"));
        }

        db.collection("artifacts").document(appId)
                .collection("users").document(userId)
                .collection("bonds")
                .whereIn("number", bondNumbers)
                .get()
                .addOnSuccessListener(snapshot -> {
                    HashSet<String> existing = new HashSet<>();
                    for (var doc : snapshot.getDocuments()) {
                        existing.add(doc.getString("number"));
                    }

                    List<Map<String, Object>> toSave = new ArrayList<>();
                    for (Map<String, Object> b : ocrBondsList) {
                        if (!existing.contains(b.get("number"))) {
                            toSave.add(b);
                        }
                    }

                    if (toSave.isEmpty()) {
                        Toast.makeText(this, "Bond already exist", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Save non-duplicates
                    WriteBatch batch = db.batch();
                    for (Map<String, Object> b : toSave) {
                        String bondNumber = (String) b.get("number");
                        batch.set(db.collection("artifacts").document(appId)
                                .collection("users").document(userId)
                                .collection("bonds").document(bondNumber), b);
                    }

                    batch.commit().addOnSuccessListener(v -> {
                        Toast.makeText(this, toSave.size() + " OCR bonds saved", Toast.LENGTH_LONG).show();
                        ivOCRPreview.setImageBitmap(null);
                        tvExtractedData.setText("");
                        ocrBondsList.clear();
                        btnSaveOCR.setEnabled(false);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving OCR bonds", Toast.LENGTH_LONG).show();
                    });

                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking duplicates", Toast.LENGTH_LONG).show();
                });
    }

    private void showDenominationDialog(HashSet<String> bondNumbers, String series) {
        String[] denomOptions = {
                "Rs. 100", "Rs. 200", "Rs. 750",
                "Rs. 1500", "Rs. 7500", "Rs. 15000",
                "Rs. 25000", "Rs. 40000"
        };

        // setMessage BILKUL MAT LIKHO setItems ke saath — Android bug
        new AlertDialog.Builder(this)
                .setTitle("Please select the correct value manually.")
                // ← setMessage() NAHI
                .setItems(denomOptions, (dialog, which) -> {
                    String selectedDenom = denomOptions[which];
                    ocrBondsList.clear();
                    for (String bn : bondNumbers) {
                        Map<String, Object> bond = new HashMap<>();
                        bond.put("number", bn);
                        bond.put("denomination", selectedDenom);
                        bond.put("series", series);
                        bond.put("drawCity", "");
                        bond.put("purchaseDate", "");
                        bond.put("trackedSince", System.currentTimeMillis());
                        ocrBondsList.add(bond);
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("✓ Found ").append(ocrBondsList.size()).append(" bond(s):\n\n");
                    for (Map<String, Object> b : ocrBondsList) {
                        sb.append("Bond:         ").append(b.get("number")).append("\n");
                        sb.append("Denomination: ").append(b.get("denomination")).append("\n");
                        if (!b.get("series").toString().isEmpty()) {
                            sb.append("Series:       ").append(b.get("series")).append("\n");
                        }
                        sb.append("\n");
                    }
                    tvExtractedData.setText(sb.toString());
                    btnSaveOCR.setEnabled(true);
                })
                .setCancelable(false)
                .show();
    }

    private void setupCityAutoComplete(AutoCompleteTextView field) {
        if (field == null) return;
        field.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() < 3) return; // 3 letters ke baad API call

                String url = "https://secure.geonames.org/searchJSON?country=PK&featureClass=P&orderby=population&maxRows=10&name_startsWith="
                        + s.toString() + "&username=prizebondtracker";

                com.android.volley.RequestQueue queue = com.android.volley.toolbox.Volley.newRequestQueue(AddBondActivity.this);

                com.android.volley.toolbox.JsonObjectRequest request =
                        new com.android.volley.toolbox.JsonObjectRequest(
                                com.android.volley.Request.Method.GET,
                                url,
                                null,
                                response -> {
                                    try {
                                        org.json.JSONArray arr = response.getJSONArray("geonames");

                                        ArrayList<String> cities = new ArrayList<>();

                                        for (int i = 0; i < arr.length(); i++) {
                                            org.json.JSONObject obj = arr.getJSONObject(i);

                                            String name = obj.getString("name");
                                            int population = obj.getInt("population");

                                            // 👉 filter (sirf real cities)
                                            if (population > 50000) {
                                                cities.add(name);
                                            }
                                        }

                                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                                AddBondActivity.this,
                                                android.R.layout.simple_dropdown_item_1line,
                                                cities
                                        );

                                        field.setAdapter(adapter);
                                        field.showDropDown();

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                },
                                error -> {
                                    if (error.networkResponse != null) {
                                        int statusCode = error.networkResponse.statusCode;
                                        Toast.makeText(AddBondActivity.this, "Error Code: " + statusCode, Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(AddBondActivity.this, "Network Error: " + error.toString(), Toast.LENGTH_LONG).show();
                                    }

                                    error.printStackTrace();
                                }
                        );

                // 🔥 IMPORTANT: increase timeout
                request.setRetryPolicy(new DefaultRetryPolicy(
                        15000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                ));

                queue.add(request);

               }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
    
}
