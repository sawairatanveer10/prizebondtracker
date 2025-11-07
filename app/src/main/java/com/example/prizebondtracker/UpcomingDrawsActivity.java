package com.example.prizebondtracker;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity to display upcoming prize bond draw schedules, synchronized in real-time
 * from Firebase Firestore, and allowing filtering.
 *
 * NOTE: This file assumes R.layout.activity_upcoming_draws and R.layout.item_draw_card
 * (and their resources) are correctly defined in your project.
 */
public class UpcomingDrawsActivity extends AppCompatActivity {

    private static final String TAG = "UpcomingDrawsActivity";
    // IMPORTANT: Use the global app ID if running in a platform like Canvas, otherwise use a default.
    private static final String APP_ID = "prize-bond-tracker-default";

    // Firebase
    private FirebaseFirestore db;
    private ListenerRegistration drawListener;
    private List<DrawScheduleItem> allDraws = new ArrayList<>();
    private List<DrawScheduleItem> filteredDraws = new ArrayList<>();

    // UI Components
    private RecyclerView rvDraws;
    private TextView tvEmptyState;
    private UpcomingDrawsAdapter adapter;
    private AutoCompleteTextView spinnerDenomination, spinnerMonth;

    // Filter Variables
    private String selectedDenomination = "All";
    private String selectedMonth = "All";
    private List<String> availableDenominations = new ArrayList<>();
    private List<String> availableMonths = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // NOTE: Replace with your actual layout file if necessary
        setContentView(R.layout.activity_upcoming_draws);

        // Initialize Firebase and UI
        initializeFirebase();
        setupToolbar();
        setupRecyclerView();
        setupFilters();
        startRealtimeListener();
    }

    /**
     * Initializes Firebase Firestore instance.
     */
    private void initializeFirebase() {
        try {
            // This is the standard way to initialize Firestore after FirebaseApp is initialized
            db = FirebaseFirestore.getInstance();
            // Optional: Set logging level for debugging firestore issues
            // FirebaseFirestore.setLogLevel(com.google.firebase.firestore.Logger.Level.DEBUG);
        } catch (Exception e) {
            Log.e(TAG, "Firebase Initialization Error: " + e.getMessage());
            Toast.makeText(this, "Failed to initialize database connection.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sets up the toolbar for navigation.
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        // Handle the back button click
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Sets up the RecyclerView and its Adapter.
     */
    private void setupRecyclerView() {
        rvDraws = findViewById(R.id.rvDraws);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        adapter = new UpcomingDrawsAdapter(filteredDraws);
        rvDraws.setAdapter(adapter);
    }

    /**
     * Sets up the filter spinners (Denomination and Month) with default options.
     */
    private void setupFilters() {
        spinnerDenomination = findViewById(R.id.spinner_denomination);
        spinnerMonth = findViewById(R.id.spinner_month);

        availableDenominations.add("All");
        availableMonths.add("All");

        // Set up adapter for Denomination
        ArrayAdapter<String> denominationAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, availableDenominations);
        spinnerDenomination.setAdapter(denominationAdapter);
        spinnerDenomination.setText("All", false);

        // Set up adapter for Month
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, availableMonths);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setText("All", false);

        // Listeners for filter changes
        spinnerDenomination.setOnItemClickListener((parent, view, position, id) -> {
            selectedDenomination = (String) parent.getItemAtPosition(position);
            applyFilters();
        });

        spinnerMonth.setOnItemClickListener((parent, view, position, id) -> {
            selectedMonth = (String) parent.getItemAtPosition(position);
            applyFilters();
        });
    }

    /**
     * Starts the real-time listener to fetch draw data from Firestore.
     * The data is fetched from the public collection: /artifacts/{appId}/public/data/draw_schedule
     */
    private void startRealtimeListener() {
        if (db == null) return;

        // Construct the Firestore path
        CollectionReference scheduleRef = db
                .collection("artifacts")
                .document(APP_ID)
                .collection("public")
                .document("data")
                .collection("draw_schedule");

        // Order by draw date to ensure the list is always chronologically correct
        Query query = scheduleRef.orderBy("drawDate", Query.Direction.ASCENDING);

        drawListener = query.addSnapshotListener((querySnapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed for draw schedule.", e);
                Toast.makeText(UpcomingDrawsActivity.this, "Error fetching draw schedule.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (querySnapshot != null) {
                // 1. Update filter options based on the complete dataset
                updateFilterOptions(querySnapshot);

                // 2. Refresh the main list of all upcoming draws
                allDraws.clear();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    DrawScheduleItem item = doc.toObject(DrawScheduleItem.class);
                    if (item != null) {
                        // Only include draws that are not yet past
                        if (item.isUpcoming()) {
                            allDraws.add(item);
                        }
                    }
                }
                Log.d(TAG, "Fetched " + allDraws.size() + " upcoming draw items.");

                // 3. Apply the current filters to the refreshed data
                applyFilters();
            }
        });
    }

    /**
     * Extracts unique denominations and months from the fetched data to populate filters.
     */
    private void updateFilterOptions(QuerySnapshot snapshot) {
        // Collect all denominations and months
        List<String> newDenominations = new ArrayList<>();
        List<String> newMonths = new ArrayList<>();
        newDenominations.add("All");
        newMonths.add("All");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            DrawScheduleItem item = doc.toObject(DrawScheduleItem.class);
            if (item != null && item.isUpcoming()) {
                // Denomination
                String denominationStr = "Rs. " + item.getDenomination();
                if (!newDenominations.contains(denominationStr)) {
                    newDenominations.add(denominationStr);
                }

                // Month
                try {
                    Date drawDate = dateFormat.parse(item.getDrawDate());
                    String monthStr = monthFormat.format(drawDate);
                    if (!newMonths.contains(monthStr)) {
                        newMonths.add(monthStr);
                    }
                } catch (ParseException e) {
                    // Ignore items with bad dates
                }
            }
        }

        // Update Denomination Filter and ensure sort order (numerically)
        Collections.sort(newDenominations, (s1, s2) -> {
            if (s1.equals("All")) return -1;
            if (s2.equals("All")) return 1;
            // Extract numeric value for comparison
            try {
                int denom1 = Integer.parseInt(s1.replace("Rs. ", ""));
                int denom2 = Integer.parseInt(s2.replace("Rs. ", ""));
                return Integer.compare(denom1, denom2);
            } catch (NumberFormatException e) {
                return 0;
            }
        });

        if (!availableDenominations.equals(newDenominations)) {
            availableDenominations.clear();
            availableDenominations.addAll(newDenominations);
            // Re-bind adapter
            ArrayAdapter<String> adapterDenom = (ArrayAdapter<String>) spinnerDenomination.getAdapter();
            if (adapterDenom != null) adapterDenom.notifyDataSetChanged();
            if (!availableDenominations.contains(selectedDenomination)) selectedDenomination = "All";
        }


        // Update Month Filter (simple alphabetical sort, can be improved)
        availableMonths.clear();
        availableMonths.add("All"); // Ensure 'All' is first
        newMonths.remove("All");
        Collections.sort(newMonths); // Sort remaining months alphabetically
        availableMonths.addAll(newMonths);

        ArrayAdapter<String> adapterMonth = (ArrayAdapter<String>) spinnerMonth.getAdapter();
        if (adapterMonth != null) adapterMonth.notifyDataSetChanged();
        if (!availableMonths.contains(selectedMonth)) selectedMonth = "All";
    }


    /**
     * Applies the current denomination and month filters to the main draw list.
     */
    private void applyFilters() {
        filteredDraws.clear();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());

        for (DrawScheduleItem item : allDraws) {
            boolean matchesDenomination = selectedDenomination.equals("All") || selectedDenomination.equals("Rs. " + item.getDenomination());
            boolean matchesMonth = true;

            if (!selectedMonth.equals("All")) {
                try {
                    Date drawDate = dateFormat.parse(item.getDrawDate());
                    String monthStr = monthFormat.format(drawDate);
                    matchesMonth = monthStr.equals(selectedMonth);
                } catch (ParseException e) {
                    matchesMonth = false; // Date parse failed, exclude it
                }
            }

            if (matchesDenomination && matchesMonth) {
                filteredDraws.add(item);
            }
        }

        adapter.notifyDataSetChanged();
        // Show/hide empty state message
        tvEmptyState.setVisibility(filteredDraws.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (drawListener != null) {
            drawListener.remove(); // Stop listening for real-time updates
        }
    }

    // =========================================================================
    // DATA MODEL (Must be public static for Firebase deserialization)
    // =========================================================================

    /**
     * Data Model for a single Prize Bond Draw Schedule Item.
     * Fields must match Firestore document keys (e.g., drawNumber, denomination, drawDate, city, status).
     */
    public static class DrawScheduleItem {
        private String id;
        private String drawNumber;
        private int denomination;
        private String drawDate; // Stored as "yyyy-MM-dd" string
        private String city;
        private String status;

        public DrawScheduleItem() {
            // Required for Firebase
        }

        // Getters and Setters (Required for Firebase)
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getDrawNumber() { return drawNumber; }
        public void setDrawNumber(String drawNumber) { this.drawNumber = drawNumber; }
        public int getDenomination() { return denomination; }
        public void setDenomination(int denomination) { this.denomination = denomination; }
        public String getDrawDate() { return drawDate; }
        public void setDrawDate(String drawDate) { this.drawDate = drawDate; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        /**
         * Utility method to determine if the draw is upcoming based on the status field.
         */
        public boolean isUpcoming() {
            return "Upcoming".equalsIgnoreCase(status);
        }

        /**
         * Calculates the number of days left until the draw date. Returns -1 on error.
         */
        public long getDaysUntilDraw() {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date drawDt = dateFormat.parse(drawDate);
                Date now = new Date();

                // Clear time components for accurate day difference (start of day)
                Calendar drawCal = Calendar.getInstance();
                drawCal.setTime(drawDt);
                drawCal.set(Calendar.HOUR_OF_DAY, 0); drawCal.set(Calendar.MINUTE, 0); drawCal.set(Calendar.SECOND, 0); drawCal.set(Calendar.MILLISECOND, 0);

                Calendar nowCal = Calendar.getInstance();
                nowCal.setTime(now);
                nowCal.set(Calendar.HOUR_OF_DAY, 0); nowCal.set(Calendar.MINUTE, 0); nowCal.set(Calendar.SECOND, 0); nowCal.set(Calendar.MILLISECOND, 0);

                long diff = drawCal.getTimeInMillis() - nowCal.getTimeInMillis();
                return diff / (24 * 60 * 60 * 1000); // Convert milliseconds to days
            } catch (ParseException e) {
                Log.e("DrawScheduleItem", "Date parsing failed: " + e.getMessage());
                return -1; // Error value
            }
        }
    }


    // =========================================================================
    // RECYCLERVIEW ADAPTER
    // =========================================================================

    /**
     * RecyclerView Adapter to bind DrawScheduleItem data to the card layout.
     */
    private class UpcomingDrawsAdapter extends RecyclerView.Adapter<UpcomingDrawsAdapter.DrawViewHolder> {

        private final List<DrawScheduleItem> drawList;
        // Date formatters for display
        private final SimpleDateFormat displayMonthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        private final SimpleDateFormat displayDayFormat = new SimpleDateFormat("dd", Locale.getDefault());
        private final SimpleDateFormat firestoreDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        public UpcomingDrawsAdapter(List<DrawScheduleItem> drawList) {
            this.drawList = drawList;
        }

        @NonNull
        @Override
        public DrawViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // NOTE: Replace with your actual item layout file if necessary
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_item_draw_card, parent, false);
            return new DrawViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DrawViewHolder holder, int position) {
            DrawScheduleItem item = drawList.get(position);

            // Populate text views
            holder.tvDenominationFull.setText("Prize Bond Rs. " + item.getDenomination());
            holder.tvDrawNumber.setText(item.getDrawNumber());
            holder.tvDrawCity.setText(item.getCity());
            holder.tvDenominationShort.setText("Rs. " + item.getDenomination());

            try {
                // Parse the Firestore date string
                Date drawDate = firestoreDateFormat.parse(item.getDrawDate());

                // Set date components
                holder.tvDrawMonth.setText(displayMonthFormat.format(drawDate).toUpperCase(Locale.getDefault()));
                holder.tvDrawDay.setText(displayDayFormat.format(drawDate));

            } catch (ParseException e) {
                // Handle date parsing failure
                holder.tvDrawMonth.setText("N/A");
                holder.tvDrawDay.setText("--");
            }

            // Update Countdown Button
            long daysLeft = item.getDaysUntilDraw();
            if (daysLeft > 0) {
                holder.btnCountdown.setText(daysLeft + " Days Left");
                holder.btnCountdown.setVisibility(View.VISIBLE);
                holder.btnCountdown.setBackgroundColor(getResources().getColor(R.color.accent_blue)); // Assuming R.color.accent_blue is defined
            } else if (daysLeft == 0) {
                holder.btnCountdown.setText("Drawing Today!");
                holder.btnCountdown.setVisibility(View.VISIBLE);
                holder.btnCountdown.setBackgroundColor(getResources().getColor(R.color.primary_green)); // Highlight today's draw
            } else {
                holder.btnCountdown.setVisibility(View.GONE); // Draw has passed
            }

            // Set a listener for the Countdown/Reminder button
            holder.btnCountdown.setOnClickListener(v -> {
                // This is where you would link to a Reminder/Notification system
                Toast.makeText(v.getContext(), "Reminder set for Rs." + item.getDenomination() + " draw on " + item.getDrawDate(), Toast.LENGTH_LONG).show();
            });
        }

        @Override
        public int getItemCount() {
            return drawList.size();
        }

        /**
         * ViewHolder class for the RecyclerView.
         */
        public class DrawViewHolder extends RecyclerView.ViewHolder {
            TextView tvDrawMonth, tvDrawDay, tvDenominationShort, tvDenominationFull, tvDrawNumber, tvDrawCity;
            MaterialButton btnCountdown;

            public DrawViewHolder(@NonNull View itemView) {
                super(itemView);
                // Map components from R.layout.item_draw_card
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
