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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvSignUp, tvForgot;
    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvForgot = findViewById(R.id.tvForgot);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(v -> loginUser());

        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            finish();
        });

        tvForgot.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });


        // ✅ Secure Auto Login Check
        if (mAuth.getCurrentUser() != null) {
            checkIfBlockedAndProceed(mAuth.getCurrentUser().getUid());
        }
    }

    // 🔐 LOGIN METHOD
    private void loginUser() {

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter password");
            return;
        }

        progressDialog.setMessage("Logging in...");
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        String uid = mAuth.getCurrentUser().getUid();
                        checkIfBlockedAndProceed(uid);

                    } else {

                        progressDialog.dismiss();

                        if (task.getException() != null) {

                            String errorCode = task.getException().getClass().getSimpleName();

                            if (errorCode.equals("FirebaseAuthInvalidUserException")) {

                                Toast.makeText(this,
                                        "Account not found. Please register first.",
                                        Toast.LENGTH_LONG).show();

                            } else if (errorCode.equals("FirebaseAuthInvalidCredentialsException")) {

                                Toast.makeText(this,
                                        "Incorrect password.",
                                        Toast.LENGTH_LONG).show();

                            } else {

                                Toast.makeText(this,
                                        "Login failed. Please try again.",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    // 🚫 CHECK BLOCKED STATUS
    private void checkIfBlockedAndProceed(String uid) {

        progressDialog.setMessage("Checking account...");
        progressDialog.show();

        db.collection("blocked_users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    progressDialog.dismiss();

                    if (snapshot.exists()) {

                        // ❌ USER IS BLOCKED
                        mAuth.signOut();
                        Toast.makeText(LoginActivity.this,
                                "You are blocked by admin.",
                                Toast.LENGTH_LONG).show();

                    } else {

                        // ✅ USER NOT BLOCKED
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


}
