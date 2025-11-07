package com.example.prizebondtracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.materialswitch.MaterialSwitch;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileSettingsActivity";

    // FIX: Define a compilable constant for the App ID.
    // In a production environment, this value must be set dynamically.
    private static final String APP_ID = "prizebond-tracker-app-id";

    // UI Elements
    private ShapeableImageView ivProfilePicture;
    private MaterialButton btnChangePhoto, btnUpdateProfile, btnLogout;
    private TextInputEditText etFullName, etEmail, etCityRegion;
    private MaterialSwitch switchNotifications, switchDataVisibility;
    private View btnChangePassword, btnForgotPassword;

    // Firebase Instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Handle case where user is not logged in
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_LONG).show();
            // Redirect to Login/Splash screen (assuming it's called AuthActivity)
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        userId = currentUser.getUid();


        // 2. Initialize Views
        initViews();
        setupToolbar();

        // 3. Load User Data
        loadUserData();

        // 4. Setup Click Listeners
        btnUpdateProfile.setOnClickListener(v -> updateProfile());
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        // Correct Logic: Change Password uses the dialog to verify current password
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Correct Logic: Forgot Password directly calls the email reset function
        btnForgotPassword.setOnClickListener(v -> sendPasswordResetEmail());

        // Listeners for Switches (Save preferences instantly)
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> saveAppPreference("notifications_enabled", isChecked));
        switchDataVisibility.setOnCheckedChangeListener((buttonView, isChecked) -> saveAppPreference("ai_data_sharing", isChecked));

        // Placeholder for Photo change (requires activity result setup, which is complex for a single file example)
        btnChangePhoto.setOnClickListener(v -> Toast.makeText(this, "Photo change feature is unavailable in this sample.", Toast.LENGTH_SHORT).show());
    }

    private void initViews() {
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        btnLogout = findViewById(R.id.btnLogout);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etCityRegion = findViewById(R.id.etCityRegion);
        btnChangePassword = findViewById(R.id.btnChangePassword); // This is a standard Button
        btnForgotPassword = findViewById(R.id.btnForgotPassword); // This is a standard Button
        switchNotifications = findViewById(R.id.switchNotifications);
        switchDataVisibility = findViewById(R.id.switchDataVisibility);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Profile Settings");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Loads user profile data from FirebaseAuth and Firestore.
     */
    private void loadUserData() {
        if (currentUser == null) return;

        // Load data from Firebase Auth (Name, Email)
        etFullName.setText(currentUser.getDisplayName());
        etEmail.setText(currentUser.getEmail());

        // Load remaining data from Firestore (City/Region, Switches)
        // Using APP_ID instead of the global __app_id
        DocumentReference userDocRef = db.collection("artifacts").document(APP_ID)
                .collection("users").document(userId)
                .collection("profile").document("info");

        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                etCityRegion.setText(documentSnapshot.getString("city_region"));
                switchNotifications.setChecked(documentSnapshot.getBoolean("notifications_enabled") != null ? documentSnapshot.getBoolean("notifications_enabled") : true);
                switchDataVisibility.setChecked(documentSnapshot.getBoolean("ai_data_sharing") != null ? documentSnapshot.getBoolean("ai_data_sharing") : false);
            }
            Log.d(TAG, "User data loaded from Firestore.");
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading user data from Firestore: " + e.getMessage());
        });
    }

    /**
     * Updates profile name in Firebase Auth and profile details in Firestore.
     */
    private void updateProfile() {
        String newFullName = etFullName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String newCityRegion = etCityRegion.getText().toString().trim();

        if (newFullName.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Full Name and Email cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) return;

        // 1. Update Full Name in Firebase Auth
        if (!newFullName.equals(currentUser.getDisplayName())) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newFullName)
                    .build();

            currentUser.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile name updated in Auth.");
                            // The Toast is given after all updates are done below
                        } else {
                            Log.e(TAG, "Error updating user name: " + Objects.requireNonNull(task.getException()).getMessage());
                            Toast.makeText(this, "Failed to update name.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        // 2. Update Email in Firebase Auth
        if (!newEmail.equals(currentUser.getEmail())) {
            // Note: Email updates require re-authentication, which is complex.
            // For simplicity, we assume the user is properly authenticated for this flow.
            currentUser.updateEmail(newEmail)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User email updated in Auth.");
                            // Must send verification email after changing email
                            currentUser.sendEmailVerification();
                            Toast.makeText(this, "Email updated. Please verify your new email.", Toast.LENGTH_LONG).show();
                        } else {
                            // Common error: requires recent login (re-authentication)
                            Log.e(TAG, "Error updating email: " + Objects.requireNonNull(task.getException()).getMessage());
                            Toast.makeText(this, "Failed to update email. Requires recent login.", Toast.LENGTH_LONG).show();
                        }
                    });
        }

        // 3. Update Custom Fields in Firestore (City/Region)
        // Using APP_ID instead of the global __app_id
        DocumentReference userDocRef = db.collection("artifacts").document(APP_ID)
                .collection("users").document(userId)
                .collection("profile").document("info");

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("city_region", newCityRegion);

        userDocRef.set(profileData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "City/Region updated in Firestore.");
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating city/region in Firestore: " + e.getMessage());
                    Toast.makeText(this, "Failed to update city/region.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates switch preferences instantly in Firestore.
     */
    private void saveAppPreference(String field, boolean isChecked) {
        if (currentUser == null) return;

        // Using APP_ID instead of the global __app_id
        DocumentReference userDocRef = db.collection("artifacts").document(APP_ID)
                .collection("users").document(userId)
                .collection("profile").document("info");

        userDocRef.update(field, isChecked)
                .addOnSuccessListener(aVoid -> Log.d(TAG, field + " preference updated."))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating " + field + " preference: " + e.getMessage()));
    }

    /**
     * Shows a confirmation dialog before logging out the user.
     */
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    // Redirect to the login/splash screen after logout
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    // Clear the back stack so the user can't hit back to get into the app
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows a dialog to collect current and new password for changing the password.
     */
    private void showChangePasswordDialog() {
        if (currentUser == null) return;

        // Inflate the custom layout for the password change dialog
        View dialogView = getLayoutInflater().inflate(R.layout.activity_dialog_change_password, null);
        TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirmNewPassword = dialogView.findViewById(R.id.etConfirmNewPassword);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String currentPassword = etCurrentPassword.getText().toString().trim();
                    String newPassword = etNewPassword.getText().toString().trim();
                    String confirmPassword = etConfirmNewPassword.getText().toString().trim();

                    if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                        Toast.makeText(this, "All password fields are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(this, "New passwords do not match.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (newPassword.length() < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    changePassword(currentPassword, newPassword);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Performs the Firebase password change operation, requiring re-authentication.
     */
    private void changePassword(String currentPassword, String newPassword) {
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "Cannot change password. Please log in with email/password.", Toast.LENGTH_LONG).show();
            return;
        }

        // 1. Re-authenticate user
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);

        currentUser.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User successfully re-authenticated.");

                        // 2. Update password
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Log.d(TAG, "User password updated.");
                                        Toast.makeText(ProfileActivity.this, "Password successfully changed!", Toast.LENGTH_LONG).show();
                                    } else {
                                        Log.e(TAG, "Password update failed: " + Objects.requireNonNull(task1.getException()).getMessage());
                                        Toast.makeText(ProfileActivity.this, "Failed to change password. Try again.", Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Log.e(TAG, "Re-authentication failed: " + Objects.requireNonNull(task.getException()).getMessage());
                        Toast.makeText(ProfileActivity.this, "Incorrect current password.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Sends a password reset email using Firebase Auth.
     * This is for the "Forgot Password" flow and does NOT require the current password.
     */
    private void sendPasswordResetEmail() {
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "Cannot reset password. Please ensure you are logged in with a valid email.", Toast.LENGTH_LONG).show();
            return;
        }

        String emailAddress = currentUser.getEmail();

        mAuth.sendPasswordResetEmail(emailAddress)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent to: " + emailAddress);
                        // Using a custom dialog instead of a simple toast for a better user experience
                        new AlertDialog.Builder(this)
                                .setTitle("Password Reset Sent")
                                .setMessage("A password reset link has been successfully sent to your registered email: " + emailAddress + ". Please check your inbox (and spam folder) to continue.")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        Log.e(TAG, "Failed to send reset email: " + Objects.requireNonNull(task.getException()).getMessage());
                        Toast.makeText(ProfileActivity.this, "Failed to send reset email. Check your connection.", Toast.LENGTH_LONG).show();
                    }
                });
    }
}
