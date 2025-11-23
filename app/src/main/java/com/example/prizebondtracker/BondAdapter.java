package com.example.prizebondtracker;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BondAdapter extends RecyclerView.Adapter<BondAdapter.BondViewHolder> {

    private List<Bond> bondList;
    private Context context;

    public BondAdapter(List<Bond> bondList, Context ctx) {
        this.bondList = bondList;
        this.context = ctx;
    }

    @NonNull
    @Override
    public BondViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bond_card, parent, false);
        return new BondViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BondViewHolder holder, int position) {
        Bond b = bondList.get(position);

        holder.tvBondNumber.setText(b.getNumber());
        holder.tvDenomination.setText(b.getDenomination());
        holder.tvPurchaseDate.setText("Purchased: " + b.getPurchaseDate());
        holder.tvSeries.setText("Series: " + b.getSeries());
        holder.tvDrawCity.setText("Draw City: " + b.getDrawCity());

        holder.btnMore.setOnClickListener(v -> showOptionsBottomSheet(b));
    }

    @Override
    public int getItemCount() {
        return bondList == null ? 0 : bondList.size();
    }

    public void updateList(List<Bond> list) {
        this.bondList = list;
        notifyDataSetChanged();
    }

    // ---------------------------------------------------------------------
    // ⭐ SHOW INSTAGRAM-STYLE BOTTOM SHEET (EDIT / DELETE)
    // ---------------------------------------------------------------------
    // Show Instagram-style options safely
    private void showOptionsBottomSheet(Bond bond) {
        if (!(context instanceof Activity)) return; // Safety check

        try {
            BottomSheetDialog bottomSheet = new BottomSheetDialog(context);
            View sheetView = LayoutInflater.from(context).inflate(
                    R.layout.bottomsheet_bond_options,
                    null, false
            );
            bottomSheet.setContentView(sheetView);

            // Safe findViewById with null check
            View btnEdit = sheetView.findViewById(R.id.btnEditBond);
            View btnDelete = sheetView.findViewById(R.id.btnDeleteBond);

            if (btnEdit != null)
                btnEdit.setOnClickListener(v -> {
                    bottomSheet.dismiss();
                    showEditBondSheet(bond);
                });

            if (btnDelete != null)
                btnDelete.setOnClickListener(v -> {
                    bottomSheet.dismiss();
                    confirmDelete(bond);
                });

            bottomSheet.show();
        } catch (Exception e) {
            Toast.makeText(context, "Error opening options: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------
    // ⭐ DELETE BOND FROM FIRESTORE USING DOCUMENT ID
    // ---------------------------------------------------------------------
    private void confirmDelete(Bond bond) {

        new MaterialAlertDialogBuilder(context)
                .setTitle("Delete Bond")
                .setMessage("Are you sure you want to delete this bond?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBondFromFirestore(bond))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBondFromFirestore(Bond bond) {

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("artifacts").document("default-app-id")
                .collection("users").document(userId)
                .collection("bonds").document(bond.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    bondList.remove(bond);
                    notifyDataSetChanged();
                });
    }

    // ---------------------------------------------------------------------
    // ⭐ EDIT BOND (UPDATE FIRESTORE DOC)
    // ---------------------------------------------------------------------
    private void showEditBondSheet(Bond bond) {

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context)
                .inflate(R.layout.bottomsheet_edit_bond, null);
        dialog.setContentView(view);

        EditText etNumber = view.findViewById(R.id.etBondNumber);
        EditText etDenom = view.findViewById(R.id.etBondDenomination);
        EditText etCity = view.findViewById(R.id.etBondCity);
        EditText etSeries = view.findViewById(R.id.etBondSeries);
        EditText etPurchase = view.findViewById(R.id.etBondPurchaseDate);

        MaterialButton btnUpdate = view.findViewById(R.id.btnUpdateBond);

        // PREFILL FIELDS
        etNumber.setText(bond.getNumber());
        etDenom.setText(bond.getDenomination());
        etCity.setText(bond.getDrawCity());
        etSeries.setText(bond.getSeries());
        etPurchase.setText(bond.getPurchaseDate());

        // 📅 DATE PICKER
        etPurchase.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog picker = new DatePickerDialog(
                    context,
                    (DatePicker view1, int year, int month, int day) -> {
                        month += 1;
                        String d = (day < 10 ? "0" + day : day) + "/" +
                                (month < 10 ? "0" + month : month) + "/" + year;
                        etPurchase.setText(d);
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        btnUpdate.setOnClickListener(v -> {

            String newNum = etNumber.getText().toString();
            String newDenom = etDenom.getText().toString();
            String newCity = etCity.getText().toString();
            String newSeries = etSeries.getText().toString();
            String newPurchase = etPurchase.getText().toString();

            Map<String, Object> updated = new HashMap<>();
            updated.put("number", newNum);
            updated.put("denomination", newDenom);
            updated.put("drawCity", newCity);
            updated.put("series", newSeries);
            updated.put("purchaseDate", newPurchase);

            updateBondInFirestore(bond.getId(), updated);

            // update local object
            bond.setNumber(newNum);
            bond.setDenomination(newDenom);
            bond.setDrawCity(newCity);
            bond.setSeries(newSeries);
            bond.setPurchaseDate(newPurchase);

            notifyDataSetChanged();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateBondInFirestore(String docId, Map<String, Object> updatedMap) {

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("artifacts").document("default-app-id")
                .collection("users").document(userId)
                .collection("bonds").document(docId)
                .update(updatedMap);
    }

    // ---------------------------------------------------------------------
    // ViewHolder
    // ---------------------------------------------------------------------
    static class BondViewHolder extends RecyclerView.ViewHolder {
        TextView tvBondNumber, tvDenomination, tvPurchaseDate, tvSeries, tvDrawCity;
        ImageButton btnMore;

        BondViewHolder(View itemView) {
            super(itemView);

            tvBondNumber = itemView.findViewById(R.id.tvBondNumber);
            tvDenomination = itemView.findViewById(R.id.tvDenomination);
            tvPurchaseDate = itemView.findViewById(R.id.tvPurchaseDate);
            tvSeries = itemView.findViewById(R.id.tvSeries);
            tvDrawCity = itemView.findViewById(R.id.tvDrawCity);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
}
