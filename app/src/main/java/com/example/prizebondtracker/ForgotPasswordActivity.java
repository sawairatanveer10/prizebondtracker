package com.example.prizebondtracker;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputLayout emailLayout;
    private Button btnVerify;

    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailLayout = findViewById(R.id.emailLayout);
        etEmail     = findViewById(R.id.etEmail);
        btnVerify   = findViewById(R.id.btnVerify);

        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // ✅ Clear error as user types
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                emailLayout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // ✅ Validate on focus lost
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String email = getEmail();
                if (!email.isEmpty()) validateEmail(email);
            }
        });

        btnVerify.setOnClickListener(v -> sendResetLink());
    }

    // ─────────────────────────────────────────────
    // 🔥 SEND FIREBASE RESET LINK
    // ─────────────────────────────────────────────
    private void sendResetLink() {

        String email = getEmail();

        if (!validateEmail(email)) return;

        progressDialog.setMessage("Sending reset link...");
        progressDialog.show();

        // ✅ DEFAULT FIREBASE METHOD (NO Dynamic Links)
        FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {

                    progressDialog.dismiss();

                    Toast.makeText(this,
                            "Reset email sent! Check your inbox.",
                            Toast.LENGTH_LONG).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    progressDialog.dismiss();

                    Toast.makeText(this,
                            e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
    // ─────────────────────────────────────────────
    // ✅ EMAIL VALIDATION
    // ─────────────────────────────────────────────
    private boolean validateEmail(String email) {

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required.");
            return false;
        }

        if (email.contains(" ")) {
            emailLayout.setError("Email must not contain spaces.");
            return false;
        }

        if (email.length() > 254) {
            emailLayout.setError("Email must not exceed 254 characters.");
            return false;
        }

        int atCount = 0;
        for (char c : email.toCharArray()) if (c == '@') atCount++;
        if (atCount != 1) {
            emailLayout.setError("Enter a valid email address.");
            return false;
        }

        String localPart  = email.substring(0, email.indexOf('@'));
        String domainPart = email.substring(email.indexOf('@') + 1);

        if (localPart.isEmpty()) {
            emailLayout.setError("Enter a valid email address.");
            return false;
        }

        if (localPart.startsWith(".") || localPart.endsWith(".")) {
            emailLayout.setError("Email must not start or end with a dot before @.");
            return false;
        }

        if (email.contains("..")) {
            emailLayout.setError("Email must not contain consecutive dots.");
            return false;
        }

        if (domainPart.isEmpty() || !domainPart.contains(".")) {
            emailLayout.setError("Enter a valid email address.");
            return false;
        }

        String[] domainLabels = domainPart.split("\\.");
        for (String label : domainLabels) {
            if (label.isEmpty()) {
                emailLayout.setError("Enter a valid email address.");
                return false;
            }
            if (Character.isDigit(label.charAt(0))) {
                emailLayout.setError("Email domain is not valid (e.g. use gmail.com, not 1gmail.com).");
                return false;
            }
            if (label.startsWith("-") || label.endsWith("-")) {
                emailLayout.setError("Email domain must not start or end with a hyphen.");
                return false;
            }
        }

        String tld = domainLabels[domainLabels.length - 1];
        if (!tld.matches("[a-zA-Z]{2,}")) {
            emailLayout.setError("Email must have a valid ending (e.g. .com, .net, .org).");
            return false;
        }

        if (!email.matches("^[a-zA-Z0-9._%+\\-]+@([a-zA-Z][a-zA-Z0-9\\-]*\\.)+[a-zA-Z]{2,}$")) {
            emailLayout.setError("Enter a valid email address (e.g. user@gmail.com).");
            return false;
        }

        emailLayout.setError(null);
        return true;
    }

    // ─────────────────────────────────────────────
    // 🛠 HELPER
    // ─────────────────────────────────────────────
    private String getEmail() {
        return etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
    }
}