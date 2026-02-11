package com.example.prizebondtracker;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private Button btnVerify;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmail);
        btnVerify = findViewById(R.id.btnVerify);

        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        btnVerify.setOnClickListener(v -> verifyEmail());
    }

    private void verifyEmail() {

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            return;
        }

        progressDialog.setMessage("Verifying email...");
        progressDialog.show();

        // 🔥 Directly send reset email (Professional approach)
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    progressDialog.dismiss();

                    if (task.isSuccessful()) {

                        Toast.makeText(this,
                                "Reset link has been sent.",
                                Toast.LENGTH_LONG).show();

                        finish(); // back to login

                    } else {

                        Toast.makeText(this,
                                "Failed to send reset link.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }


    private void sendResetLink(String email) {

        progressDialog.setMessage("Sending reset link...");

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    progressDialog.dismiss();

                    if (task.isSuccessful()) {

                        Toast.makeText(this,
                                "Password reset link sent to your email.",
                                Toast.LENGTH_LONG).show();

                        finish(); // return to login

                    } else {

                        Toast.makeText(this,
                                "Failed to send reset link.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
