package com.example.prizebondtracker;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout emailLayout, passwordLayout;
    private Button btnLogin;
    private TextView tvSignUp, tvForgot;
    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // 🔒 Track failed login attempts
    private int failedAttempts = 0;
    private static final int MAX_ATTEMPTS = 5;
    private boolean isLockedOut = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailLayout    = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        etEmail        = findViewById(R.id.etEmail);
        etPassword     = findViewById(R.id.etPassword);
        btnLogin       = findViewById(R.id.btnLogin);
        tvSignUp       = findViewById(R.id.tvSignUp);
        tvForgot       = findViewById(R.id.tvForgot);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // ✅ Clear errors as soon as user starts typing
        etEmail.addTextChangedListener(new ClearErrorWatcher(emailLayout));
        etPassword.addTextChangedListener(new ClearErrorWatcher(passwordLayout));

        // ✅ Validate email when user leaves the field (on focus lost)
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String email = getTextFrom(etEmail);
                if (!email.isEmpty()) {
                    validateEmail(email);
                }
            }
        });

        // ✅ Validate password when user leaves the field (on focus lost)
        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String password = getTextFrom(etPassword);
                if (!password.isEmpty()) {
                    validatePassword(password);
                }
            }
        });

        btnLogin.setOnClickListener(v -> loginUser());

        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            finish();
        });

        tvForgot.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class))
        );

        // ✅ Secure Auto Login Check
        if (mAuth.getCurrentUser() != null) {
            checkIfBlockedAndProceed(mAuth.getCurrentUser().getUid());
        }
    }

    // ─────────────────────────────────────────────
    // 🔐 LOGIN METHOD
    // ─────────────────────────────────────────────
    private void loginUser() {

        if (isLockedOut) {
            Toast.makeText(this,
                    "Too many failed attempts. Please wait and try again.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String email    = getTextFrom(etEmail);
        String password = getTextFrom(etPassword);

        // ✅ MUST FILL BOTH FIELDS
        if (TextUtils.isEmpty(email) && TextUtils.isEmpty(password)) {
            emailLayout.setError("Email is required.");
            passwordLayout.setError("Password is required.");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required.");
            return;
        }

        // ✅ Validate email (important)
        boolean emailOk = validateEmail(email);

        // ✅ Simple password validation
        boolean passwordOk = validatePassword(password);

        if (!emailOk || !passwordOk) return;

        progressDialog.setMessage("Logging in...");
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        failedAttempts = 0;
                        String uid = mAuth.getCurrentUser().getUid();
                        checkIfBlockedAndProceed(uid);

                    } else {

                        progressDialog.dismiss();
                        failedAttempts++;

                        if (failedAttempts >= MAX_ATTEMPTS) {
                            isLockedOut = true;
                            btnLogin.setEnabled(false);

                            Toast.makeText(this,
                                    "Account locked after " + MAX_ATTEMPTS + " failed attempts. Try again later.",
                                    Toast.LENGTH_LONG).show();

                            btnLogin.postDelayed(() -> {
                                isLockedOut = false;
                                failedAttempts = 0;
                                btnLogin.setEnabled(true);

                                Toast.makeText(this,
                                        "You can try again now.",
                                        Toast.LENGTH_SHORT).show();
                            }, 30_000);
                            return;
                        }

                        int remaining = MAX_ATTEMPTS - failedAttempts;
                        if (failedAttempts >= 3) {
                            Toast.makeText(this,
                                    "Warning: " + remaining + " attempt(s) remaining.",
                                    Toast.LENGTH_LONG).show();
                        }

                        // ✅ PROFESSIONAL ERROR (no info leak)
                        Toast.makeText(this,
                                "Invalid email or password.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ─────────────────────────────────────────────
    // ✅ EMAIL VALIDATION
    // ─────────────────────────────────────────────
    private boolean validateEmail(String email) {

        // 1. Empty check
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required.");
            return false;
        }

        // 2. No spaces
        if (email.contains(" ")) {
            emailLayout.setError("Email must not contain spaces.");
            return false;
        }

        // 3. Max length (RFC 5321)
        if (email.length() > 254) {
            emailLayout.setError("Email must not exceed 254 characters.");
            return false;
        }

        // 4. Exactly one @ symbol
        int atCount = 0;
        for (char c : email.toCharArray()) if (c == '@') atCount++;
        if (atCount != 1) {
            emailLayout.setError("Enter a valid email address.");
            return false;
        }

        String localPart  = email.substring(0, email.indexOf('@'));
        String domainPart = email.substring(email.indexOf('@') + 1);

        // 5a. Local part must not be empty
        if (localPart.isEmpty()) {
            emailLayout.setError("Enter a valid email address.");
            return false;
        }

        // 5b. Local part: no leading or trailing dot
        if (localPart.startsWith(".") || localPart.endsWith(".")) {
            emailLayout.setError("Email must not start or end with a dot before @.");
            return false;
        }

        // 5c. No consecutive dots anywhere in email
        if (email.contains("..")) {
            emailLayout.setError("Email must not contain consecutive dots.");
            return false;
        }

        // 6 & 7. Each domain label check (split by dot)
        //   - Cannot start or end with a hyphen  (e.g. -gmail.com or gmail-.com)
        //   - Cannot start with a digit          (e.g. 1gmail.com, 2mail.org)
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
            // ❌ Domain label starts with a digit → "1gmail", "2mail"
            if (Character.isDigit(label.charAt(0))) {
                emailLayout.setError("Email domain is not valid (e.g. use gmail.com, not 1gmail.com).");
                return false;
            }
            // ❌ Domain label starts or ends with hyphen → "-gmail" or "gmail-"
            if (label.startsWith("-") || label.endsWith("-")) {
                emailLayout.setError("Email domain must not start or end with a hyphen.");
                return false;
            }
        }

        // 8. TLD (last label) must be letters only, min 2 chars
        //    Blocks: .123, .1com, .c
        String tld = domainLabels[domainLabels.length - 1];
        if (!tld.matches("[a-zA-Z]{2,}")) {
            emailLayout.setError("Email must have a valid ending (e.g. .com, .net, .org).");
            return false;
        }

        // 9. Full regex as final gate
        //    Local part : letters, digits, . _ % + -
        //    Domain     : letters and hyphens only per label (digits allowed inside, not at start)
        //    TLD        : letters only, 2+ chars
        String emailRegex = "^[a-zA-Z0-9._%+\\-]+@([a-zA-Z][a-zA-Z0-9\\-]*\\.)+[a-zA-Z]{2,}$";
        if (!email.matches(emailRegex)) {
            emailLayout.setError("Enter a valid email address (e.g. user@gmail.com).");
            return false;
        }

        emailLayout.setError(null);
        return true;
    }
    // ─────────────────────────────────────────────
    // ✅ PASSWORD VALIDATION
    // ─────────────────────────────────────────────
    private boolean validatePassword(String password) {

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required.");
            return false;
        }

        if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters.");
            return false;
        }

        if (password.length() > 128) {
            passwordLayout.setError("Password must not exceed 128 characters.");
            return false;
        }

        if (password.contains(" ")) {
            passwordLayout.setError("Password must not contain spaces.");
            return false;
        }

        passwordLayout.setError(null);
        return true;
    }
    // ─────────────────────────────────────────────
    // 🚫 CHECK BLOCKED STATUS
    // ─────────────────────────────────────────────
    private void checkIfBlockedAndProceed(String uid) {

        progressDialog.setMessage("Checking account...");
        progressDialog.show();

        db.collection("blocked_users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    progressDialog.dismiss();

                    if (snapshot.exists()) {
                        mAuth.signOut();
                        Toast.makeText(LoginActivity.this,
                                "You are blocked by admin.",
                                Toast.LENGTH_LONG).show();

                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Login successful",
                                Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ─────────────────────────────────────────────
    // 🛠 HELPER: Get trimmed text safely
    // ─────────────────────────────────────────────
    private String getTextFrom(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    // ─────────────────────────────────────────────
    // 🛠 HELPER: TextWatcher to clear errors on typing
    // ─────────────────────────────────────────────
    private static class ClearErrorWatcher implements TextWatcher {
        private final TextInputLayout layout;

        ClearErrorWatcher(TextInputLayout layout) {
            this.layout = layout;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            layout.setError(null); // Clear error as user types
        }
        @Override public void afterTextChanged(Editable s) {}
    }
}