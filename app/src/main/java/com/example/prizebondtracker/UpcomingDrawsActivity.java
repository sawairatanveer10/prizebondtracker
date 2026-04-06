package com.example.prizebondtracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UpcomingDrawsActivity extends Fragment {

    private List<DrawScheduleItem> allDraws = new ArrayList<>();
    private List<DrawScheduleItem> filteredDraws = new ArrayList<>();

    private RecyclerView rvDraws;
    private TextView tvEmptyState;
    private UpcomingDrawsAdapter adapter;
    private AutoCompleteTextView spinnerDenomination, spinnerMonth;

    private String selectedDenomination = "All";
    private String selectedMonth = "All";

    // Formats matching your Python Database ("dd-MM-yyyy")
    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    public UpcomingDrawsActivity() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_upcoming_draws, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        rvDraws = view.findViewById(R.id.rvDraws);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        spinnerDenomination = view.findViewById(R.id.spinner_denomination);
        spinnerMonth = view.findViewById(R.id.spinner_month);

        adapter = new UpcomingDrawsAdapter(filteredDraws);
        rvDraws.setAdapter(adapter);

        setupFilters();
        fetchDrawsFromFirestore();

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), HomeActivity.class));
                getActivity().finish();
            }
        });
    }

    private void setupFilters() {
        // Denominations matching your Python list
        String[] denominations = {"All", "Rs. 100", "Rs. 200", "Rs. 750", "Rs. 1500"};
        String[] months = {"All", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};

        ArrayAdapter<String> denomAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, denominations);
        spinnerDenomination.setAdapter(denomAdapter);
        spinnerDenomination.setText("All", false);

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, months);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setText("All", false);

        spinnerDenomination.setOnItemClickListener((parent, v, pos, id) -> {
            selectedDenomination = (String) parent.getItemAtPosition(pos);
            applyFilters();
        });

        spinnerMonth.setOnItemClickListener((parent, v, pos, id) -> {
            selectedMonth = (String) parent.getItemAtPosition(pos);
            applyFilters();
        });
    }

    private void fetchDrawsFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetching from your "all_draws" collection
        db.collection("all_draws")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allDraws.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DrawScheduleItem item = document.toObject(DrawScheduleItem.class);
                        allDraws.add(item);
                    }
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("FirestoreError", e.getMessage());
                });
    }

    private void applyFilters() {
        filteredDraws.clear();
        SimpleDateFormat monthNameFormat = new SimpleDateFormat("MMMM", Locale.getDefault());

        for (DrawScheduleItem item : allDraws) {
            // Check Denomination (e.g., "Rs. 750" contains "750")
            boolean matchesDenom = selectedDenomination.equals("All") ||
                    selectedDenomination.contains(item.getBondValue());

            // Check Month
            boolean matchesMonth = true;
            if (!selectedMonth.equals("All")) {
                try {
                    Date date = dbDateFormat.parse(item.getDate());
                    String monthName = monthNameFormat.format(date);
                    matchesMonth = monthName.equalsIgnoreCase(selectedMonth);
                } catch (ParseException e) {
                    matchesMonth = false;
                }
            }

            if (matchesDenom && matchesMonth) {
                filteredDraws.add(item);
            }
        }

        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredDraws.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // --- Data Model matching your Python script fields ---
    public static class DrawScheduleItem {
        private String bondValue; // Maps to "bondValue" in Firestore
        private String date;      // Maps to "date" in Firestore
        private String city;      // Maps to "city" in Firestore
        private String year;      // Maps to "year" in Firestore

        public DrawScheduleItem() {} // Necessary for Firestore

        public String getBondValue() { return bondValue; }
        public String getDate() { return date; }
        public String getCity() { return city; }
        public String getYear() { return year; }
    }

    // --- Adapter ---
    public class UpcomingDrawsAdapter extends RecyclerView.Adapter<UpcomingDrawsAdapter.DrawViewHolder> {
        private final List<DrawScheduleItem> drawList;

        public UpcomingDrawsAdapter(List<DrawScheduleItem> drawList) {
            this.drawList = drawList;
        }

        @NonNull
        @Override
        public DrawViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_item_draw_card, parent, false);
            return new DrawViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DrawViewHolder holder, int position) {
            DrawScheduleItem item = drawList.get(position);

            holder.tvDenominationFull.setText("Prize Bond Rs. " + item.getBondValue());
            holder.tvDenominationShort.setText("Rs. " + item.getBondValue());
            holder.tvDrawCity.setText(item.getCity());
            holder.tvDrawNumber.setText("None"); // You can add draw numbers to your Python script later

            try {
                Date date = dbDateFormat.parse(item.getDate());

                holder.tvDrawMonth.setText(
                        new SimpleDateFormat("MMM", Locale.getDefault())
                                .format(date)
                                .toUpperCase()
                );

                holder.tvDrawDay.setText(
                        new SimpleDateFormat("dd", Locale.getDefault())
                                .format(date)
                );

                // Remove time from today for accurate comparison
                Calendar todayCal = Calendar.getInstance();
                todayCal.set(Calendar.HOUR_OF_DAY, 0);
                todayCal.set(Calendar.MINUTE, 0);
                todayCal.set(Calendar.SECOND, 0);
                todayCal.set(Calendar.MILLISECOND, 0);

                Date today = todayCal.getTime();

                long diff = date.getTime() - today.getTime();
                long days = diff / (24 * 60 * 60 * 1000);

                if (days > 0) {
                    holder.btnCountdown.setText(days + " Days Left");
                } else if (days == 0) {
                    holder.btnCountdown.setText("Today");
                } else {
                    holder.btnCountdown.setText("Held");
                }

            } catch (Exception e) {
                holder.tvDrawMonth.setText("N/A");
                holder.tvDrawDay.setText("--");
                holder.btnCountdown.setText("Unknown");
            }
        }

        @Override
        public int getItemCount() { return drawList.size(); }

        public class DrawViewHolder extends RecyclerView.ViewHolder {
            TextView tvDrawMonth, tvDrawDay, tvDenominationShort, tvDenominationFull, tvDrawNumber, tvDrawCity;
            MaterialButton btnCountdown;

            public DrawViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDrawMonth = itemView.findViewById(R.id.tvDrawMonth);
                tvDrawDay = itemView.findViewById(R.id.tvDrawDay);
                tvDenominationShort = itemView.findViewById(R.id.tvDenominationShort);
                tvDenominationFull = itemView.findViewById(R.id.tvDenominationFull);
                tvDrawNumber = itemView.findViewById(R.id.tvDrawNumber);
                tvDrawCity = itemView.findViewById(R.id.tvDrawCity);
                btnCountdown = itemView.findViewById(R.id.btnCountdown);
            }
        }
    }
}