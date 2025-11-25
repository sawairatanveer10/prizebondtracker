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
    private static final int PICK_IMAGE = 1001;

    private ShapeableImageView ivProfilePicture;
    private MaterialButton btnEditProfile, btnChangePhoto, btnUpdateProfile, btnLogout;
    private Button btnChangePassword, btnForgotPassword;
    private TextInputEditText etFullName, etEmail, etCityRegion;
    private MaterialSwitch switchNotifications, switchDataVisibility;
    private View editProfileCard;
    private TextView tvDisplayName, tvDisplayEmail;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private String userId;

    private Uri imageUri;

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
        btnChangePhoto.setOnClickListener(v -> openGallery());
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnForgotPassword.setOnClickListener(v -> sendPasswordResetEmail());
    }

    private void initViews(View view) {
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        btnUpdateProfile = view.findViewById(R.id.btnUpdateProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnForgotPassword = view.findViewById(R.id.btnForgotPassword);
        etFullName = view.findViewById(R.id.etFullName);
        etEmail = view.findViewById(R.id.etEmail);
        etCityRegion = view.findViewById(R.id.etCityRegion);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        switchDataVisibility = view.findViewById(R.id.switchDataVisibility);
        editProfileCard = view.findViewById(R.id.editProfileCard);
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvDisplayEmail = view.findViewById(R.id.tvDisplayEmail);
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

                switchNotifications.setChecked(doc.getBoolean("notifications_enabled") != null ? doc.getBoolean("notifications_enabled") : true);
                switchDataVisibility.setChecked(doc.getBoolean("ai_data_sharing") != null ? doc.getBoolean("ai_data_sharing") : false);
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to load user data: " + e.getMessage()));
    }

    private void toggleEditCard() {
        if(editProfileCard.getVisibility() == View.GONE){
            editProfileCard.setVisibility(View.VISIBLE);
            editProfileCard.animate().alpha(1f).setDuration(200);
            btnEditProfile.setText("Cancel");
        } else {
            editProfileCard.animate().alpha(0f).setDuration(150).withEndAction(() -> editProfileCard.setVisibility(View.GONE));
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
                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showChangePasswordDialog(){
        View view = getLayoutInflater().inflate(R.layout.activity_dialog_change_password,null);
        TextInputEditText etCurrent = view.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNew = view.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = view.findViewById(R.id.etConfirmNewPassword);

        new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton("Save",(dialog,which)->{
                    String current = etCurrent.getText().toString().trim();
                    String newPass = etNew.getText().toString().trim();
                    String confirm = etConfirm.getText().toString().trim();

                    if(current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()){
                        Toast.makeText(getActivity(),"All fields required",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(!newPass.equals(confirm)){
                        Toast.makeText(getActivity(),"Passwords do not match",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    changePassword(current,newPass);
                })
                .setNegativeButton("Cancel",null)
                .show();
    }

    private void changePassword(String currentPassword,String newPassword){
        if(currentUser==null || currentUser.getEmail()==null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(),currentPassword);

        currentUser.reauthenticate(credential).addOnCompleteListener(task->{
            if(task.isSuccessful()){
                currentUser.updatePassword(newPassword).addOnCompleteListener(task1->{
                    if(task1.isSuccessful()){
                        Toast.makeText(getActivity(),"Password changed successfully!",Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getActivity(),"Failed: "+task1.getException().getMessage(),Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(getActivity(),"Current password incorrect",Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendPasswordResetEmail(){
        if(currentUser==null || currentUser.getEmail()==null) return;

        String email = currentUser.getEmail();
        mAuth.sendPasswordResetEmail(email).addOnSuccessListener(aVoid->{
            new AlertDialog.Builder(getActivity())
                    .setTitle("Email Sent")
                    .setMessage("Password reset link has been sent to:\n"+email)
                    .setPositiveButton("OK",null)
                    .show();
        }).addOnFailureListener(e-> Toast.makeText(getActivity(),"Failed: "+e.getMessage(),Toast.LENGTH_LONG).show());
    }

    private void openGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_IMAGE && resultCode==getActivity().RESULT_OK && data!=null){
            imageUri = data.getData();
            ivProfilePicture.setImageURI(imageUri);
        }
    }

    private void showLogoutConfirmation(){
        new AlertDialog.Builder(getActivity())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes",(dialog, which)->{
                    mAuth.signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel",null)
                .show();
    }
}