package com.example.prizebondtracker;

import android.content.Intent;
import android.icu.util.Calendar;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
        loadNextDrawDate();

        // Set click listeners
        btnQuickAdd.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddBondActivity.class)));
        btnViewRecommendations.setOnClickListener(v -> startActivity(new Intent(getActivity(), BudgetSuggestionsActivity.class)));
        btnNotifications.setOnClickListener(v -> startActivity(new Intent(getActivity(), NotificationActivity.class)));

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
        String appId = "default-app-id";

        FirebaseFirestore.getInstance()
                .collection("artifacts")
                .document(appId)
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        tvWelcome.setText("Welcome Back, User");
                        return;
                    }

                    String name = doc.getString("name");

                    if (name == null || name.trim().isEmpty()) {
                        tvWelcome.setText("Welcome Back, User");
                    } else {
                        tvWelcome.setText("Welcome Back, " + name);
                    }

                })
                .addOnFailureListener(e -> {
                    tvWelcome.setText("Welcome Back, User");
                });
    }


    private void loadBondCount() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            tvTotalBonds.setText("0 Bonds");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String appId = "default-app-id"; // use same appId as saving

        FirebaseFirestore.getInstance()
                .collection("artifacts")
                .document(appId)
                .collection("users")
                .document(uid)
                .collection("bonds")
                .get()
                .addOnSuccessListener(query -> {
                    int count = query.size();
                    tvTotalBonds.setText(count + " Bonds");
                })
                .addOnFailureListener(e -> tvTotalBonds.setText("0 Bonds"));
    }

    private void loadNextDrawDate() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("all_draws")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Date nearestDate = null;
                    SimpleDateFormat dbFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());

                    // Today's date with time cleared
                    Calendar todayCal = Calendar.getInstance();
                    todayCal.set(Calendar.HOUR_OF_DAY, 0);
                    todayCal.set(Calendar.MINUTE, 0);
                    todayCal.set(Calendar.SECOND, 0);
                    todayCal.set(Calendar.MILLISECOND, 0);
                    Date today = todayCal.getTime();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String dateStr = doc.getString("date");
                        if (dateStr != null) {
                            try {
                                Date drawDate = dbFormat.parse(dateStr);
                                if (drawDate != null && drawDate.after(today)) {
                                    if (nearestDate == null || drawDate.before(nearestDate)) {
                                        nearestDate = drawDate;
                                    }
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (nearestDate != null) {
                        tvNextDraw.setText(displayFormat.format(nearestDate));
                    } else {
                        tvNextDraw.setText("No upcoming draws");
                    }

                })
                .addOnFailureListener(e -> tvNextDraw.setText("Error loading date"));
    }

}
