package com.example.prizebondtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeFragment extends Fragment {

    // Header Views
    private ImageView ivMenuIcon;
    private ImageButton btnNotifications;
    private TextView tvAppTitle, tvWelcome, tvSubtitle, tvTotalBonds, tvNextDraw;

    // Fragment Content Views
    private MaterialButton btnQuickAdd, btnViewRecommendations;
    private MaterialCardView cardAI;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Initialize Header
        ivMenuIcon = view.findViewById(R.id.ivMenuIcon);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        tvAppTitle = view.findViewById(R.id.tvAppTitle);
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvSubtitle = view.findViewById(R.id.tvSubtitle);
        tvTotalBonds = view.findViewById(R.id.tvTotalBonds);
        tvNextDraw = view.findViewById(R.id.tvNextDraw);

        // Initialize Fragment Content
        btnQuickAdd = view.findViewById(R.id.btnQuickAdd);
        btnViewRecommendations = view.findViewById(R.id.btnViewRecommendations);
        cardAI = view.findViewById(R.id.cardAI);

        // Load dynamic data
        loadUserData();
        loadBondCount();

        // Set click listeners
        btnQuickAdd.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddBondActivity.class)));
        btnViewRecommendations.setOnClickListener(v -> startActivity(new Intent(getActivity(), AiRecommendActivity.class)));
        btnNotifications.setOnClickListener(v -> startActivity(new Intent(getActivity(), NotificationsActivity.class)));

        ivMenuIcon.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity)getActivity()).showCustomDropdownMenu(v);
            }
        });
    }

    private void loadUserData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            tvWelcome.setText("Welcome Back, Guest");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("artifacts")
                .document("default-app-id")
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String name = "User";
                    if (doc.exists()) {
                        String fetchedName = doc.getString("fullName");
                        if (fetchedName != null && !fetchedName.isEmpty()) name = fetchedName;
                    }
                    tvWelcome.setText("Welcome Back, " + name);
                })
                .addOnFailureListener(e -> tvWelcome.setText("Welcome Back, User"));
    }

    private void loadBondCount() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            tvTotalBonds.setText("0 Bonds");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("artifacts")
                .document("default-app-id")
                .collection("users")
                .document(uid)
                .collection("bonds")
                .get()
                .addOnSuccessListener(query -> tvTotalBonds.setText(query.size() + " Bonds"))
                .addOnFailureListener(e -> tvTotalBonds.setText("0 Bonds"));
    }
}
