package com.example.prizebondtracker;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword;
    private TextInputLayout nameLayout, emailLayout, passwordLayout;

    private Button btnSignUp;
    private TextView tvGoToLogin;
    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    private final String appId = "default-app-id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);

        btnSignUp = findViewById(R.id.btnSignUp);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");
        progressDialog.setCancelable(false);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnSignUp.setOnClickListener(v -> createAccount());

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void createAccount() {

        String name     = getText(etName);
        String email    = getText(etEmail);
        String password = getText(etPassword);

        // ✅ Name
        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Full name is required.");
            return;
        }

        // ✅ Email
        if (!validateEmail(email)) return;

        // ✅ Password
        if (!validatePassword(password)) return;

        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {

                            String uid = user.getUid();

                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("uid", uid);
                            userMap.put("name", name);
                            userMap.put("email", email);
                            userMap.put("createdAt", System.currentTimeMillis());
                            userMap.put("blocked", false);

                            firestore.collection("artifacts")
                                    .document(appId)
                                    .collection("users")
                                    .document(uid)
                                    .set(userMap)
                                    .addOnSuccessListener(aVoid -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(this, LoginActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }

                    } else {
                        progressDialog.dismiss();
                        String message = "Signup failed. Try again.";

                        if (task.getException() != null) {
                            String error = task.getException().getMessage();

                            if (error.contains("The email address is already in use")) {
                                message = "This email is already registered. Please login instead.";
                            } else if (error.contains("badly formatted")) {
                                message = "Invalid email format.";
                            } else if (error.contains("password is invalid")) {
                                message = "Password is too weak.";
                            } else {
                                message = error;
                            }
                        }
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // 🔹 Helper
    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    // 🔥 EMAIL VALIDATION (YOUR FULL VERSION)
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
            emailLayout.setError("Email must not start or end with a dot.");
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
            if (label.isEmpty()) return false;

            if (Character.isDigit(label.charAt(0))) {
                emailLayout.setError("Invalid domain.");
                return false;
            }

            if (label.startsWith("-") || label.endsWith("-")) {
                emailLayout.setError("Invalid domain.");
                return false;
            }
        }

        String tld = domainLabels[domainLabels.length - 1];
        if (!tld.matches("[a-zA-Z]{2,}")) {
            emailLayout.setError("Invalid email ending.");
            return false;
        }

        String emailRegex = "^[a-zA-Z0-9._%+\\-]+@([a-zA-Z][a-zA-Z0-9\\-]*\\.)+[a-zA-Z]{2,}$";
        if (!email.matches(emailRegex)) {
            emailLayout.setError("Enter valid email (e.g. user@gmail.com).");
            return false;
        }

        emailLayout.setError(null);
        return true;
    }

    // 🔥 PASSWORD VALIDATION (YOUR FULL VERSION)
    private boolean validatePassword(String password) {

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required.");
            return false;
        }

        if (password.length() < 6) {
            passwordLayout.setError("Minimum 6 characters required.");
            return false;
        }

        if (password.length() > 128) {
            passwordLayout.setError("Too long password.");
            return false;
        }

        if (!password.matches(".*[A-Z].*")) {
            passwordLayout.setError("Must contain uppercase letter.");
            return false;
        }

        if (!password.matches(".*[a-z].*")) {
            passwordLayout.setError("Must contain lowercase letter.");
            return false;
        }

        if (!password.matches(".*[0-9].*")) {
            passwordLayout.setError("Must contain a number.");
            return false;
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            passwordLayout.setError("Must contain special character.");
            return false;
        }

        if (password.contains(" ")) {
            passwordLayout.setError("No spaces allowed.");
            return false;
        }

        passwordLayout.setError(null);
        return true;
    }
}