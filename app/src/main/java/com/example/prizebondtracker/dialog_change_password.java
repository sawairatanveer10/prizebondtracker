package com.example.prizebondtracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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

    // Firebase (or other Auth) variables would be initialized here
    // private FirebaseAuth mAuth;
    // private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_change_password);

        // 1. Initialize Firebase Auth (or your backend client)
        // mAuth = FirebaseAuth.getInstance();
        // currentUser = mAuth.getCurrentUser();

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
        // changePassword(currentPassword, newPassword);

        // Placeholder for successful attempt (remove when actual logic is implemented)
        Toast.makeText(this, "Validation successful. Initiating password update...", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handles local input validation.
     */
    private boolean validateFields(String currentPass, String newPass, String confirmPass) {
        boolean valid = true;

        if (TextUtils.isEmpty(currentPass)) {
            tilCurrentPassword.setError("Required");
            valid = false;
        }

        if (TextUtils.isEmpty(newPass) || newPass.length() < 6) {
            tilNewPassword.setError("Password must be at least 6 characters.");
            valid = false;
        }

        if (!newPass.equals(confirmPass)) {
            tilConfirmNewPassword.setError("Passwords do not match.");
            valid = false;
        }

        return valid;
    }


    /**
     * NOTE: This is where your Firebase or backend authentication logic would go.
     * Firebase requires re-authentication before updating the password for security.
     * @param currentPassword The user's current password for re-authentication.
     * @param newPassword The new password to set.
     */
    /*
    private void changePassword(String currentPassword, String newPassword) {
        // 1. Create a credential using the user's current password
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);

        // 2. Re-authenticate the user
        currentUser.reauthenticate(credential)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // 3. If re-authentication succeeds, update the password
                    currentUser.updatePassword(newPassword)
                        .addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                Toast.makeText(ChangePasswordActivity.this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                                finish(); // Close the activity
                            } else {
                                Toast.makeText(ChangePasswordActivity.this, "Error updating password.", Toast.LENGTH_SHORT).show();
                            }
                        });
                } else {
                    // Re-authentication failed (likely due to wrong current password)
                    tilCurrentPassword.setError("Incorrect current password.");
                    Toast.makeText(ChangePasswordActivity.this, "Authentication failed. Check your current password.", Toast.LENGTH_LONG).show();
                }
            });
    }
    */
}
