package com.example.prizebondtracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddBondActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 201;
    private static final int REQUEST_IMAGE_PICK = 202;

    private FirebaseFirestore db;
    private String userId;
    private final String appId = "default-app-id";

    private TabLayout tabLayout;
    private TextInputEditText etBondNumber, etPurchaseDate, etBondSeries, etDrawCity;
    private TextInputEditText etStartBondNumber, etEndBondNumber;
    private TextInputEditText etPurchaseDateBulk, etBondSeriesBulk, etDrawCityBulk;
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
        if (!isValidBondNumber(start) || !isValidBondNumber(end)) {
            Toast.makeText(this, "Invalid bond format", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> bonds = generateSequentialBonds(start, end);
        if (bonds.isEmpty()) {
            Toast.makeText(this, "Invalid range", Toast.LENGTH_LONG).show();
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
                        bond.put("series", etBondSeriesBulk.getText().toString().trim());
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
    private void pickImage(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK || data==null) return;

        Bitmap bitmap = null;
        if(requestCode==REQUEST_IMAGE_CAPTURE){
            bitmap = (Bitmap) data.getExtras().get("data");
        } else if(requestCode==REQUEST_IMAGE_PICK){
            Uri uri = data.getData();
            try { bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri); }
            catch (IOException e){ e.printStackTrace(); }
        }

        if(bitmap!=null){
            ivOCRPreview.setImageBitmap(bitmap);
            processOCR(bitmap);
        }
    }

    private void processOCR(Bitmap bitmap){
        InputImage image = InputImage.fromBitmap(bitmap,0);
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener(this::extractBondsFromText)
                .addOnFailureListener(e-> Toast.makeText(this,"OCR failed",Toast.LENGTH_LONG).show());
    }

    private void extractBondsFromText(Text visionText){
        ocrBondsList.clear();
        String text = visionText.getText();

        Pattern bondPattern = Pattern.compile("[A-Z]-?\\d{6}");
        Matcher mBond = bondPattern.matcher(text);
        HashSet<String> bondNumbers = new HashSet<>();
        while(mBond.find()){
            String raw = mBond.group();
            String prefix = raw.substring(0,1).toUpperCase();
            String digits = raw.replaceAll("[^0-9]","");
            bondNumbers.add(prefix+"-"+digits);
        }

        String denomination = extractDenomination(text);
        if(denomination.isEmpty()){
            Toast.makeText(this,"Denomination not found in OCR",Toast.LENGTH_LONG).show();
            btnSaveOCR.setEnabled(false);
            return;
        }

        String series = extractSeries(text);

        for(String bn:bondNumbers){
            Map<String,Object> bond = new HashMap<>();
            bond.put("number",bn);
            bond.put("denomination",denomination);
            bond.put("series",series);
            bond.put("drawCity",""); // optional
            bond.put("purchaseDate",""); // optional
            bond.put("trackedSince",System.currentTimeMillis());
            ocrBondsList.add(bond);
        }

        if(ocrBondsList.isEmpty()){
            tvExtractedData.setText("No valid bonds found");
            btnSaveOCR.setEnabled(false);
        } else {
            StringBuilder sb = new StringBuilder();
            for(Map<String,Object> b:ocrBondsList){
                sb.append("Bond: ").append(b.get("number")).append("\n")
                        .append("Denomination: ").append(b.get("denomination")).append("\n")
                        .append("Series: ").append(b.get("series")).append("\n\n");
            }
            tvExtractedData.setText(sb.toString());
            btnSaveOCR.setEnabled(true); // enable save after extraction
        }
    }


    private String extractDenomination(String text){
        text = text.toUpperCase();
        for(String d:DENOMINATIONS){
            if(text.contains(d.replace("Rs. ","")) || text.contains(d.replace(".",""))){
                return d;
            }
        }
        return "";
    }

    private String extractSeries(String text){
        Pattern p = Pattern.compile("(Series\\s*:?\\s*(\\d{1,3}))|(\\b\\d{1,3}\\b\\s*Series)",Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if(m.find()) return m.group().replaceAll("[^0-9]","");
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


}
