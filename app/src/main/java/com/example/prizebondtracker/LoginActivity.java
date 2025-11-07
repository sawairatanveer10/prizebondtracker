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

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvSignUp, tvForgot;
    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;

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
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        mAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> loginUser());
        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            finish();
        });

        tvForgot.setOnClickListener(v -> {
            // Basic forgot password dialog
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(LoginActivity.this, "Enter email and tap forgot again", Toast.LENGTH_SHORT).show();
                return;
            }
            progressDialog.setMessage("Sending reset email...");
            progressDialog.show();
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Reset email sent", Toast.LENGTH_SHORT).show();
                } else {
                    String err = task.getException() != null ? task.getException().getMessage() : "Failed to send reset";
                    Toast.makeText(LoginActivity.this, err, Toast.LENGTH_LONG).show();
                }
            });
        });

        // If user already logged in, send to Home directly
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
        }
    }

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

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                finish();
            } else {
                String err = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                Toast.makeText(LoginActivity.this, err, Toast.LENGTH_LONG).show();
            }
        });
    }
}
