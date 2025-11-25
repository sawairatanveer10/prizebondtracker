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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword;
    private Button btnSignUp;
    private TextView tvGoToLogin;
    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    // 🔹 Use the same appId as AddBondActivity for consistency
    private final String appId = "default-app-id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
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
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            etName.setError("Enter full name");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (!isValidEmail(email)) {
            etEmail.setError("Enter a valid email");
            return;
        }

        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();

                            // 🔹 Create user profile map
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("uid", uid);
                            userMap.put("name", name);
                            userMap.put("email", email);
                            userMap.put("createdAt", System.currentTimeMillis());

                            // 🔹 Save inside /artifacts/{appId}/users/{uid}
                            firestore.collection("artifacts")
                                    .document(appId)
                                    .collection("users")
                                    .document(uid)
                                    .set(userMap)
                                    .addOnSuccessListener(aVoid -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(SignupActivity.this, "Account created", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SignupActivity.this, HomeActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(SignupActivity.this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(SignupActivity.this, "Signup failed (no user).", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        progressDialog.dismiss();
                        String err = task.getException() != null ? task.getException().getMessage() : "Signup failed";
                        Toast.makeText(SignupActivity.this, err, Toast.LENGTH_LONG).show();
                    }
                });


    }
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

}
