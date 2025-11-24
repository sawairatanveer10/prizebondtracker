package com.example.prizebondtracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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
    private List<String> availableDenominations = new ArrayList<>();
    private List<String> availableMonths = new ArrayList<>();

    public UpcomingDrawsActivity() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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
        loadDummyDraws();
    }

    private void setupFilters() {
        availableDenominations.add("All");
        availableMonths.add("All");

        ArrayAdapter<String> denomAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, availableDenominations);
        spinnerDenomination.setAdapter(denomAdapter);
        spinnerDenomination.setText("All", false);

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, availableMonths);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setText("All", false);

        spinnerDenomination.setOnItemClickListener((parent, view, position, id) -> {
            selectedDenomination = (String) parent.getItemAtPosition(position);
            applyFilters();
        });

        spinnerMonth.setOnItemClickListener((parent, view, position, id) -> {
            selectedMonth = (String) parent.getItemAtPosition(position);
            applyFilters();
        });
    }

    private void loadDummyDraws() {
        allDraws.clear();

        allDraws.add(new DrawScheduleItem("1", "98", 750, "2025-11-15", "Karachi", "Upcoming"));
        allDraws.add(new DrawScheduleItem("2", "99", 1500, "2025-12-10", "Lahore", "Upcoming"));
        allDraws.add(new DrawScheduleItem("3", "100", 7500, "2026-01-05", "Rawalpindi", "Upcoming"));
        allDraws.add(new DrawScheduleItem("4", "101", 15000, "2026-02-20", "Peshawar", "Upcoming"));

        // Denominations
        availableDenominations.clear();
        availableDenominations.add("All");
        availableDenominations.add("Rs. 750");
        availableDenominations.add("Rs. 1500");
        availableDenominations.add("Rs. 7500");
        availableDenominations.add("Rs. 15000");

        // Months
        availableMonths.clear();
        availableMonths.add("All");
        availableMonths.add("November");
        availableMonths.add("December");
        availableMonths.add("January");
        availableMonths.add("February");

        applyFilters();
    }

    private void applyFilters() {
        filteredDraws.clear();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());

        for (DrawScheduleItem item : allDraws) {
            boolean matchesDenomination = selectedDenomination.equals("All") ||
                    selectedDenomination.equals("Rs. " + item.getDenomination());

            boolean matchesMonth = true;
            if (!selectedMonth.equals("All")) {
                try {
                    Date drawDate = dateFormat.parse(item.getDrawDate());
                    String monthStr = monthFormat.format(drawDate);
                    matchesMonth = monthStr.equalsIgnoreCase(selectedMonth);
                } catch (ParseException e) {
                    matchesMonth = false;
                }
            }

            if (matchesDenomination && matchesMonth) filteredDraws.add(item);
        }

        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredDraws.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ------------------- Data Models -------------------

    public static class DrawScheduleItem {
        private String id;
        private String drawNumber;
        private int denomination;
        private String drawDate;
        private String city;
        private String status;

        public DrawScheduleItem() {}

        public DrawScheduleItem(String id, String drawNumber, int denomination, String drawDate, String city, String status) {
            this.id = id;
            this.drawNumber = drawNumber;
            this.denomination = denomination;
            this.drawDate = drawDate;
            this.city = city;
            this.status = status;
        }

        public String getId() { return id; }
        public String getDrawNumber() { return drawNumber; }
        public int getDenomination() { return denomination; }
        public String getDrawDate() { return drawDate; }
        public String getCity() { return city; }
        public String getStatus() { return status; }
        public boolean isUpcoming() { return "Upcoming".equalsIgnoreCase(status); }
    }

    // ------------------- Adapter -------------------

    public class UpcomingDrawsAdapter extends RecyclerView.Adapter<UpcomingDrawsAdapter.DrawViewHolder> {

        private final List<DrawScheduleItem> drawList;

        public UpcomingDrawsAdapter(List<DrawScheduleItem> drawList) {
            this.drawList = drawList;
        }

        @NonNull
        @Override
        public DrawViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_item_draw_card, parent, false);
            return new DrawViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DrawViewHolder holder, int position) {
            DrawScheduleItem item = drawList.get(position);

            // Set denomination
            holder.tvDenominationFull.setText("Prize Bond Rs. " + item.getDenomination());
            holder.tvDenominationShort.setText("Rs. " + item.getDenomination());

            // Set draw number & city
            holder.tvDrawNumber.setText(item.getDrawNumber());
            holder.tvDrawCity.setText(item.getCity());

            // Convert date to abbreviation for left card (NOV, DEC)
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = dateFormat.parse(item.getDrawDate());
                SimpleDateFormat monthAbbrev = new SimpleDateFormat("MMM", Locale.getDefault());
                SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());

                holder.tvDrawMonth.setText(monthAbbrev.format(date).toUpperCase());
                holder.tvDrawDay.setText(dayFormat.format(date));
            } catch (Exception e) {
                holder.tvDrawMonth.setText("N/A");
                holder.tvDrawDay.setText("--");
            }

            holder.btnCountdown.setText("Upcoming"); // For dummy
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
