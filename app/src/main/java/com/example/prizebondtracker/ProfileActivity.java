package com.example.prizebondtracker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final String APP_ID = "prizebond-tracker-app-id";
    private static final int PICK_IMAGE = 1001;

    // UI Elements
    private ShapeableImageView ivProfilePicture;
    private MaterialButton btnEditProfile, btnChangePhoto, btnUpdateProfile, btnLogout;
    private TextInputEditText etFullName, etEmail, etCityRegion;
    private MaterialSwitch switchNotifications, switchDataVisibility;
    private View btnChangePassword, btnForgotPassword, editProfileCard;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String userId;

    private Uri imageUri; // selected image
    private TextView tvDisplayName, tvDisplayEmail;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated. Please login.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        userId = currentUser.getUid();

        initViews();
        setupToolbar();
        loadUserData();

        // Edit Profile toggle
        btnEditProfile.setOnClickListener(v -> toggleEditCard());

        // Update profile button
        btnUpdateProfile.setOnClickListener(v -> updateProfile());

        // Change photo
        btnChangePhoto.setOnClickListener(v -> openGallery());

        // Logout
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        // Password actions
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnForgotPassword.setOnClickListener(v -> sendPasswordResetEmail());

        // Switch preferences
        switchNotifications.setOnCheckedChangeListener((b, checked) -> saveAppPreference("notifications_enabled", checked));
        switchDataVisibility.setOnCheckedChangeListener((b, checked) -> saveAppPreference("ai_data_sharing", checked));
    }

    private void initViews() {
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        btnLogout = findViewById(R.id.btnLogout);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etCityRegion = findViewById(R.id.etCityRegion);
        switchNotifications = findViewById(R.id.switchNotifications);
        switchDataVisibility = findViewById(R.id.switchDataVisibility);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        editProfileCard = findViewById(R.id.editProfileCard);
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvDisplayEmail = findViewById(R.id.tvDisplayEmail);

    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Profile Settings");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------- Load User Data ----------
    private void loadUserData() {
        if (currentUser == null) return;

        // Always show email from FirebaseAuth
        String email = currentUser.getEmail();
        tvDisplayEmail.setText(email);
        etEmail.setText(email);

        // Firestore reference
        DocumentReference userDocRef = db.collection("artifacts")
                .document(APP_ID)
                .collection("users")
                .document(userId)
                .collection("profile")
                .document("info");

        userDocRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {

                // Load username from Firestore
                String name = doc.getString("name"); // <-- use your exact field name
                if (name != null && !name.isEmpty()) {
                    tvDisplayName.setText(name);
                    etFullName.setText(name);
                } else {
                    // fallback
                    tvDisplayName.setText(email);
                    etFullName.setText("");
                }

                // Load city
                etCityRegion.setText(doc.getString("city_region"));

                // Load switches
                switchNotifications.setChecked(doc.getBoolean("notifications_enabled") != null ? doc.getBoolean("notifications_enabled") : true);
                switchDataVisibility.setChecked(doc.getBoolean("ai_data_sharing") != null ? doc.getBoolean("ai_data_sharing") : false);
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to load user data: " + e.getMessage()));
    }


    // ---------- Toggle Edit Card ----------
    private void toggleEditCard() {
        if(editProfileCard.getVisibility() == View.GONE){
            editProfileCard.setVisibility(View.VISIBLE);
            editProfileCard.animate().alpha(1f).setDuration(200);
            btnEditProfile.setText("Cancel");
        } else {
            editProfileCard.animate().alpha(0f).setDuration(150).withEndAction(() -> editProfileCard.setVisibility(View.GONE));
            btnEditProfile.setText("Edit Profile");
        }
    }

    // ---------- Update Profile ----------
    private void updateProfile() {
        String newName = etFullName.getText().toString().trim();
        String newCity = etCityRegion.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder();
        builder.setDisplayName(newName); // optional fallback

        // Save name & city in Firestore
        DocumentReference userDocRef = db.collection("artifacts")
                .document(APP_ID)
                .collection("users")
                .document(userId)
                .collection("profile")
                .document("info");

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);       // store in Firestore
        updates.put("city_region", newCity);

        userDocRef.set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    currentUser.updateProfile(builder.build());
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    toggleEditCard();
                    tvDisplayName.setText(newName); // immediately update header
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }


    private void saveProfileWithoutImage(UserProfileChangeRequest.Builder builder, String city){
        currentUser.updateProfile(builder.build()).addOnSuccessListener(aVoid -> {
            saveCityToFirestore(city);
            Toast.makeText(this,"Profile updated successfully!", Toast.LENGTH_SHORT).show();
            toggleEditCard();
        });
    }

    private void uploadImageAndSave(UserProfileChangeRequest.Builder builder, String city){
        StorageReference ref = storage.getReference("profile_photos/" + userId + ".jpg");
        ref.putFile(imageUri).addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
            builder.setPhotoUri(uri);
            currentUser.updateProfile(builder.build()).addOnSuccessListener(aVoid -> {
                saveCityToFirestore(city);
                Glide.with(this).load(uri).into(ivProfilePicture);
                Toast.makeText(this,"Profile updated successfully!", Toast.LENGTH_SHORT).show();
                toggleEditCard();
            });
        })).addOnFailureListener(e -> Toast.makeText(this,"Image upload failed",Toast.LENGTH_SHORT).show());
    }

    private void saveCityToFirestore(String city){
        DocumentReference userDocRef = db.collection("artifacts")
                .document(APP_ID)
                .collection("users")
                .document(userId)
                .collection("profile")
                .document("info");

        Map<String,Object> data = new HashMap<>();
        data.put("city_region", city);
        userDocRef.set(data, SetOptions.merge());
    }

    // ---------- Switch Preferences ----------
    private void saveAppPreference(String field, boolean isChecked){
        if(currentUser == null) return;
        DocumentReference userDocRef = db.collection("artifacts")
                .document(APP_ID)
                .collection("users")
                .document(userId)
                .collection("profile")
                .document("info");

        Map<String,Object> updates = new HashMap<>();
        updates.put(field, isChecked);
        userDocRef.set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, field + " updated: " + isChecked))
                .addOnFailureListener(e -> Log.e(TAG,"Failed to update "+field+": "+e.getMessage()));
    }

    // ---------- Open Gallery ----------
    private void openGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_IMAGE && resultCode==RESULT_OK && data!=null){
            imageUri = data.getData();
            ivProfilePicture.setImageURI(imageUri);
        }
    }

    // ---------- Logout ----------
    private void showLogoutConfirmation(){
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes",(dialog, which)->{
                    mAuth.signOut();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel",null)
                .show();
    }

    // ---------- Change Password ----------
    private void showChangePasswordDialog(){
        View view = getLayoutInflater().inflate(R.layout.activity_dialog_change_password,null);
        TextInputEditText etCurrent = view.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNew = view.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = view.findViewById(R.id.etConfirmNewPassword);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(view)
                .setPositiveButton("Save",(dialog,which)->{
                    String current = etCurrent.getText().toString().trim();
                    String newPass = etNew.getText().toString().trim();
                    String confirm = etConfirm.getText().toString().trim();

                    if(current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()){
                        Toast.makeText(this,"All fields required",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(!newPass.equals(confirm)){
                        Toast.makeText(this,"Passwords do not match",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    changePassword(current,newPass);
                })
                .setNegativeButton("Cancel",null)
                .show();
    }

    private void changePassword(String currentPassword,String newPassword){
        if(currentUser==null || currentUser.getEmail()==null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(),currentPassword);

        currentUser.reauthenticate(credential).addOnCompleteListener(task->{
            if(task.isSuccessful()){
                currentUser.updatePassword(newPassword).addOnCompleteListener(task1->{
                    if(task1.isSuccessful()){
                        Toast.makeText(this,"Password changed successfully!",Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,"Failed: "+task1.getException().getMessage(),Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(this,"Current password incorrect",Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---------- Forgot Password ----------
    private void sendPasswordResetEmail(){
        if(currentUser==null || currentUser.getEmail()==null) return;

        String email = currentUser.getEmail();
        mAuth.sendPasswordResetEmail(email).addOnSuccessListener(aVoid -> {
            new AlertDialog.Builder(this)
                    .setTitle("Email Sent")
                    .setMessage("Password reset link has been sent to:\n"+email)
                    .setPositiveButton("OK",null)
                    .show();
        }).addOnFailureListener(e-> Toast.makeText(this,"Failed: "+e.getMessage(),Toast.LENGTH_LONG).show());
    }
}
