package com.example.prizebondtracker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final String APP_ID = "default-app-id";

    private ShapeableImageView ivProfilePicture;
    private MaterialButton btnEditProfile, btnUpdateProfile, btnLogout;
    private Button btnChangePassword, btnForgotPassword;
    private TextInputEditText etFullName, etEmail, etCityRegion;
    private MaterialSwitch switchNotifications, switchDataVisibility;
    private View editProfileCard;
    private TextView tvDisplayName, tvDisplayEmail;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private String userId;

    // Flag to prevent listener firing during initial load
    private boolean isLoadingData = false;

    public ProfileActivity() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getActivity(), "User not authenticated. Please login.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            return;
        }

        userId = currentUser.getUid();

        initViews(view);
        setupToolbar(view);
        loadUserData();

        btnEditProfile.setOnClickListener(v -> toggleEditCard());
        btnUpdateProfile.setOnClickListener(v -> updateProfile());
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        btnChangePassword.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), dialog_change_password.class)));

        btnForgotPassword.setOnClickListener(v -> sendPasswordResetEmail());

        // ─── App Preferences Switch Listeners ───────────────────────────────

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isLoadingData) return; // skip during data load
            savePreference("receive_draw_notification", isChecked);
            String msg = isChecked
                    ? "Draw notifications enabled"
                    : "Draw notifications disabled";
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
        });

        switchDataVisibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isLoadingData) return; // skip during data load
            savePreference("enable_ai_insight", isChecked);
            String msg = isChecked
                    ? "AI Insights enabled"
                    : "AI Insights disabled";
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
        });

        // ────────────────────────────────────────────────────────────────────

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), HomeActivity.class));
                getActivity().finish();
            }
        });
    }

    private void initViews(View view) {
        ivProfilePicture    = view.findViewById(R.id.ivProfilePicture);
        btnEditProfile      = view.findViewById(R.id.btnEditProfile);
        btnUpdateProfile    = view.findViewById(R.id.btnUpdateProfile);
        btnLogout           = view.findViewById(R.id.btnLogout);
        btnChangePassword   = view.findViewById(R.id.btnChangePassword);
        btnForgotPassword   = view.findViewById(R.id.btnForgotPassword);
        etFullName          = view.findViewById(R.id.etFullName);
        etEmail             = view.findViewById(R.id.etEmail);
        etCityRegion        = view.findViewById(R.id.etCityRegion);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        switchDataVisibility= view.findViewById(R.id.switchDataVisibility);
        editProfileCard     = view.findViewById(R.id.editProfileCard);
        tvDisplayName       = view.findViewById(R.id.tvDisplayName);
        tvDisplayEmail      = view.findViewById(R.id.tvDisplayEmail);
    }

    private void setupToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle("Profile Settings");
    }

    private void loadUserData() {
        if (currentUser == null) return;

        String email = currentUser.getEmail();
        tvDisplayEmail.setText(email);
        etEmail.setText(email);

        isLoadingData = true; // prevent switch listeners from firing

        DocumentReference userDocRef = db.collection("artifacts")
                .document(APP_ID)
                .collection("users")
                .document(userId);

        userDocRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                tvDisplayName.setText(name != null ? name : email);
                etFullName.setText(name != null ? name : "");
                etCityRegion.setText(doc.getString("city_region"));

                // ── Load saved preferences ──────────────────────────────────
                Boolean drawNotif = doc.getBoolean("receive_draw_notification");
                Boolean aiInsight = doc.getBoolean("enable_ai_insight");

                switchNotifications.setChecked(drawNotif != null && drawNotif);
                switchDataVisibility.setChecked(aiInsight != null && aiInsight);
                // ────────────────────────────────────────────────────────────
            }
            isLoadingData = false; // re-enable listeners after data loaded
        }).addOnFailureListener(e -> {
            isLoadingData = false;
            Log.e(TAG, "Failed to load user data: " + e.getMessage());
        });
    }

    /**
     * Saves a single boolean preference to Firestore.
     * Key: "receive_draw_notification" or "enable_ai_insight"
     */
    private void savePreference(String key, boolean value) {
        DocumentReference userDocRef = db.collection("artifacts")
                .document(APP_ID)
                .collection("users")
                .document(userId);

        Map<String, Object> update = new HashMap<>();
        update.put(key, value);

        userDocRef.set(update, SetOptions.merge())
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to save preference: " + e.getMessage()));
    }

    private void toggleEditCard() {
        if (editProfileCard.getVisibility() == View.GONE) {
            editProfileCard.setVisibility(View.VISIBLE);
            editProfileCard.animate().alpha(1f).setDuration(200);
            btnEditProfile.setText("Cancel");
        } else {
            editProfileCard.animate().alpha(0f).setDuration(150)
                    .withEndAction(() -> editProfileCard.setVisibility(View.GONE));
            btnEditProfile.setText("Edit Profile");
        }
    }

    private void updateProfile() {
        String newName = etFullName.getText().toString().trim();
        String newCity = etCityRegion.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(getActivity(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder();
        builder.setDisplayName(newName);

        DocumentReference userDocRef = db.collection("artifacts")
                .document(APP_ID)
                .collection("users")
                .document(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("city_region", newCity);

        userDocRef.set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    currentUser.updateProfile(builder.build());
                    Toast.makeText(getActivity(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    toggleEditCard();
                    tvDisplayName.setText(newName);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getActivity(), "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void sendPasswordResetEmail() {
        if (getActivity() == null || currentUser == null) return;
        startActivity(new Intent(getActivity(), ForgotPasswordActivity.class));
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}