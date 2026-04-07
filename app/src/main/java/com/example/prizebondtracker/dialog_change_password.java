package com.example.prizebondtracker;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
// You would need to import Firebase classes if using Firebase
// import com.google.firebase.auth.AuthCredential;
// import com.google.firebase.auth.EmailAuthProvider;
// import com.google.firebase.auth.FirebaseAuth;
// import com.google.firebase.auth.FirebaseUser;


/**
 * Activity for the user to securely change their password.
 * This process typically requires re-authenticating the user with their current password.
 */
public class dialog_change_password extends AppCompatActivity {

    private TextInputLayout tilCurrentPassword, tilNewPassword, tilConfirmNewPassword;
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private MaterialButton btnSavePassword;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_change_password);

        // 1. Initialize Firebase Auth (or your backend client)
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // 2. Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish()); // Handle back navigation

        // 3. Initialize Views
        tilCurrentPassword = findViewById(R.id.tilCurrentPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmNewPassword = findViewById(R.id.tilConfirmNewPassword);

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);


        btnSavePassword = findViewById(R.id.btnSavePassword);

        btnSavePassword.setOnClickListener(v -> {
            attemptPasswordChange();
        });

        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilNewPassword.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });


        etCurrentPassword.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilCurrentPassword.setError(null);
            }
            public void afterTextChanged(Editable s) {}
        });

        etConfirmNewPassword.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilConfirmNewPassword.setError(null);
            }
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Gathers input, validates fields, and initiates the password change process.
     */
    private void attemptPasswordChange() {
        // Reset errors
        tilCurrentPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmNewPassword.setError(null);

        // Get strings from inputs
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmNewPassword.getText().toString().trim();

        // Check if current user is logged in (Crucial check for Firebase/other auth)
        // if (currentUser == null) {
        //    Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show();
        //    return;
        // }

        if (!validateFields(currentPassword, newPassword, confirmPassword)) {
            return;
        }

        // The actual change logic
        changePassword(currentPassword, newPassword);

    }

    /**
     * Handles local input validation.
     */
    private boolean validateFields(String currentPass, String newPass, String confirmPass) {

        boolean valid = true;

        if (TextUtils.isEmpty(currentPass)) {
            tilCurrentPassword.setError("Current password required");
            valid = false;
        } else {
            tilCurrentPassword.setError(null);
        }

        if (!validatePassword(newPass, tilNewPassword)) {
            valid = false;
        }

        if (!newPass.equals(confirmPass)) {
            tilConfirmNewPassword.setError("Passwords do not match");
            valid = false;
        } else {
            tilConfirmNewPassword.setError(null);
        }

        return valid;
    }


    /**
     * NOTE: This is where your Firebase or backend authentication logic would go.
     * Firebase requires re-authentication before updating the password for security.
     * @param currentPassword The user's current password for re-authentication.
     * @param newPassword The new password to set.
     */

    private void changePassword(String currentPassword, String newPassword) {

        if (currentUser == null || currentUser.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider
                .getCredential(currentUser.getEmail(), currentPassword);

        currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                currentUser.updatePassword(newPassword).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Toast.makeText(this, "Password updated!", Toast.LENGTH_LONG).show();
                        finish(); // close screen
                    } else {
                        Toast.makeText(this, "Error: " + task1.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

            } else {
                tilCurrentPassword.setError("Wrong current password");
            }
        });
    }


    private boolean validatePassword(String password, TextInputLayout layout) {

        if (TextUtils.isEmpty(password)) {
            layout.setError("Password is required.");
            return false;
        }

        if (password.length() < 6) {
            layout.setError("Minimum 6 characters required.");
            return false;
        }

        if (password.length() > 128) {
            layout.setError("Too long password.");
            return false;
        }

        if (!password.matches(".*[A-Z].*")) {
            layout.setError("Must contain uppercase letter.");
            return false;
        }

        if (!password.matches(".*[a-z].*")) {
            layout.setError("Must contain lowercase letter.");
            return false;
        }

        if (!password.matches(".*[0-9].*")) {
            layout.setError("Must contain a number.");
            return false;
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            layout.setError("Must contain special character.");
            return false;
        }

        if (password.contains(" ")) {
            layout.setError("No spaces allowed.");
            return false;
        }

        layout.setError(null);
        return true;
    }
   
}
