package com.example.prizebondtracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MyBondsActivity extends Fragment {

    private RecyclerView recyclerView;
    private BondAdapter adapter;
    private List<Bond> bondList = new ArrayList<>();      // displayed list
    private List<Bond> fullBondList = new ArrayList<>();  // master list (all bonds)
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LinearLayout emptyStateView;
    private TextView tvBondCount;
    private EditText etSearchBonds;
    private MaterialButton btnFilter, btnSort, btnAddFirstBond;
    private final String appId = "default-app-id"; // must match AddBondActivity

    public MyBondsActivity() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_my_bonds, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.recyclerViewBonds);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        emptyStateView = view.findViewById(R.id.emptyStateView);
        tvBondCount = view.findViewById(R.id.tvBondCount);

        etSearchBonds = view.findViewById(R.id.etSearchBonds);
        btnFilter = view.findViewById(R.id.btnFilter);
        btnSort = view.findViewById(R.id.btnSort);
        btnAddFirstBond = view.findViewById(R.id.btnAddFirstBond);

        adapter = new BondAdapter(bondList, getActivity());
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        btnFilter.setOnClickListener(v -> showFilterBottomSheet());
        btnSort.setOnClickListener(v -> showSortDialog());

        etSearchBonds.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterBonds(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnAddFirstBond.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddBondActivity.class)));

        loadUserBonds();

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                // Start HomeActivity
                startActivity(new Intent(getActivity(), HomeActivity.class));
                getActivity().finish(); // optional, close current fragment host activity
            }
        });


    }

    private void loadUserBonds() {
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        db.collection("artifacts").document(appId)
                .collection("users").document(userId)
                .collection("bonds")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bondList.clear();
                    fullBondList.clear();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Bond b = new Bond(
                                    doc.getString("number"),
                                    doc.getString("denomination"),
                                    doc.getString("purchaseDate"),
                                    doc.getString("series"),
                                    doc.getString("drawCity")
                            );
                            b.setId(doc.getId()); // <-- set Firestore document ID
                            fullBondList.add(b);
                        }

                        bondList.addAll(fullBondList);
                        emptyStateView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        emptyStateView.setVisibility(View.VISIBLE);
                    }

                    tvBondCount.setText("Total Bonds: " + bondList.size());
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Failed to load bonds.", Toast.LENGTH_SHORT).show());
    }

    private void filterBonds(String query) {
        query = query == null ? "" : query.toLowerCase(Locale.getDefault()).trim();
        bondList.clear();

        if (query.isEmpty()) {
            bondList.addAll(fullBondList);
        } else {
            for (Bond b : fullBondList) {
                if ((b.getNumber() != null && b.getNumber().toLowerCase().contains(query)) ||
                        (b.getDenomination() != null && b.getDenomination().toLowerCase().contains(query)) ||
                        (b.getSeries() != null && b.getSeries().toLowerCase().contains(query)) ||
                        (b.getDrawCity() != null && b.getDrawCity().toLowerCase().contains(query)) ||
                        (b.getPurchaseDate() != null && b.getPurchaseDate().toLowerCase().contains(query))
                ) {
                    bondList.add(b);
                }
            }
        }

        adapter.notifyDataSetChanged();
        tvBondCount.setText("Total Bonds: " + bondList.size());
    }

    private void showSortDialog() {
        String[] options = {"Newest First", "Oldest First", "Denomination Low→High", "Denomination High→Low"};

        new MaterialAlertDialogBuilder(getActivity())
                .setTitle("Sort Bonds By")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: Collections.reverse(bondList); break;
                        case 1:
                            bondList.clear();
                            bondList.addAll(fullBondList);
                            break;
                        case 2:
                            Collections.sort(bondList, (a, b) -> extractDenomination(a.getDenomination()) - extractDenomination(b.getDenomination()));
                            break;
                        case 3:
                            Collections.sort(bondList, (a, b) -> extractDenomination(b.getDenomination()) - extractDenomination(a.getDenomination()));
                            break;
                    }
                    adapter.notifyDataSetChanged();
                })
                .show();
    }

    private int extractDenomination(String denom) {
        try {
            if (denom == null) return 0;
            return Integer.parseInt(denom.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(getActivity());
        View view = getLayoutInflater().inflate(R.layout.layout_filter_bonds, null);
        dialog.setContentView(view);

        AutoCompleteTextView filterDenomination = view.findViewById(R.id.filterDenomination);
        AutoCompleteTextView filterCity = view.findViewById(R.id.filterCity);
        MaterialButton btnDateRange = view.findViewById(R.id.filterDateRange);
        MaterialButton btnApply = view.findViewById(R.id.btnApplyFilters);
        MaterialButton btnReset = view.findViewById(R.id.btnResetFilters);

        String[] denoms = {"All", "Rs. 100", "Rs. 200", "Rs. 750", "Rs. 1500", "Rs. 7500", "Rs. 15000", "Rs. 25000", "Rs. 40000"};
        filterDenomination.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, denoms));
        filterDenomination.setText("All", false);

        String[] cities = {"All", "Lahore", "Karachi", "Rawalpindi", "Peshawar", "Quetta", "Multan", "Faisalabad", "Hyderabad"};
        filterCity.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, cities));
        filterCity.setText("All", false);

        final long[] dateStart = {0};
        final long[] dateEnd = {0};

        btnDateRange.setOnClickListener(v -> {
            MaterialDatePicker<androidx.core.util.Pair<Long, Long>> datePicker =
                    MaterialDatePicker.Builder.dateRangePicker()
                            .setTitleText("Select Purchase Date Range")
                            .build();

            datePicker.show(getParentFragmentManager(), "DATE_RANGE");

            datePicker.addOnPositiveButtonClickListener(selection -> {
                if (selection != null) {
                    dateStart[0] = selection.first != null ? selection.first : 0;
                    dateEnd[0] = selection.second != null ? selection.second : 0;
                    btnDateRange.setText(datePicker.getHeaderText());
                }
            });
        });

        btnReset.setOnClickListener(v -> {
            filterDenomination.setText("All", false);
            filterCity.setText("All", false);
            dateStart[0] = 0;
            dateEnd[0] = 0;

            bondList.clear();
            bondList.addAll(fullBondList);
            adapter.notifyDataSetChanged();
            tvBondCount.setText("Total Bonds: " + bondList.size());

            dialog.dismiss();
        });

        btnApply.setOnClickListener(v -> {
            String selectedDenom = filterDenomination.getText().toString();
            String selectedCity = filterCity.getText().toString();

            bondList.clear();

            for (Bond b : fullBondList) {
                boolean match = true;

                if (!"All".equals(selectedDenom) && (b.getDenomination() == null || !b.getDenomination().equals(selectedDenom))) {
                    match = false;
                }

                if (!"All".equals(selectedCity) && (b.getDrawCity() == null || !b.getDrawCity().equalsIgnoreCase(selectedCity))) {
                    match = false;
                }

                if (dateStart[0] != 0 && b.getPurchaseDate() != null && !b.getPurchaseDate().isEmpty()) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        long bondDate = sdf.parse(b.getPurchaseDate()).getTime();
                        if (bondDate < dateStart[0] || bondDate > dateEnd[0]) {
                            match = false;
                        }
                    } catch (Exception ignored) {}
                }

                if (match) bondList.add(b);
            }

            adapter.notifyDataSetChanged();
            tvBondCount.setText("Total Bonds: " + bondList.size());
            dialog.dismiss();
        });

        dialog.show();
    }
}
